package com.back.b2st.domain.member.controller;

import static com.back.b2st.domain.auth.service.AuthTestRequestBuilder.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.auth.dto.request.LoginReq;
import com.back.b2st.domain.bank.BankCode;
import com.back.b2st.domain.member.dto.request.PasswordChangeReq;
import com.back.b2st.domain.member.dto.request.RefundAccountReq;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.error.MemberErrorCode;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.member.repository.RefundAccountRepository;
import com.back.b2st.global.error.code.CommonErrorCode;
import com.back.b2st.global.test.AbstractContainerBaseTest;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class MypageControllerTest extends AbstractContainerBaseTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private RefundAccountRepository refundAccountRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void setup() {
		refundAccountRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("통합: 내 정보 조회 성공")
	void getMyInfo_success() throws Exception {
		String email = "mypage@test.com";
		Member member = Member.builder()
			.email(email)
			.password(passwordEncoder.encode("Password123!"))
			.name("마이페이지")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build();
		memberRepository.save(member);

		// 토큰 발급
		LoginReq loginRequest = buildLoginRequest(email, "Password123!");

		String loginResponse = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(loginRequest))).andReturn().getResponse().getContentAsString();

		// 토큰 추출
		JsonNode jsonNode = objectMapper.readTree(loginResponse);
		String accessToken = jsonNode.path("data").path("accessToken").asText();

		// 내 정보 조회 요청
		mockMvc.perform(get("/api/mypage/me").header("Authorization", "Bearer " + accessToken) // 토큰 필수
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			// 응답 검증
			.andExpect(jsonPath("$.data.email").value(email))
			.andExpect(jsonPath("$.data.name").value(member.getName()));
	}

	@Test
	@DisplayName("통합: 내 정보 조회 실패 - 토큰 없음")
	void getMyInfo_fail_no_token() throws Exception {
		mockMvc.perform(get("/api/mypage/me").contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(401))
			.andExpect(jsonPath("$.message").value(CommonErrorCode.UNAUTHORIZED.getMessage()));
	}

	@Test
	@DisplayName("통합: 비밀번호 변경 성공")
	void changePassword_success() throws Exception {
		String email = "pwchange@test.com";
		String oldPw = "OldPass123!";
		Member member = Member.builder()
			.email(email)
			.password(passwordEncoder.encode(oldPw))
			.name("비번변경")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build();
		memberRepository.save(member);

		// 토큰 발급
		String accessToken = getAccessToken(email, oldPw);

		// 변경 요청 빌더로 생성
		PasswordChangeReq request = buildPasswordChangeRequest(oldPw, "NewPass123!");

		// API 호출
		mockMvc.perform(patch("/api/mypage/password").header("Authorization", "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request))).andDo(print()).andExpect(status().isOk());

		// DB 검증: 새 비밀번호로 변경되었는지 확인
		Member updatedMember = memberRepository.findByEmail(email).orElseThrow();
		// matches(raw, encoded)
		if (!passwordEncoder.matches("NewPass123!", updatedMember.getPassword())) {
			throw new IllegalStateException("테스트 일회용 예외: 비밀번호가 DB에 제대로 업데이트되지 않았습니다.");
		}
	}

	@Test
	@DisplayName("통합: 비밀번호 변경 실패 - 현재 비밀번호 불일치")
	void changePassword_fail_mismatch() throws Exception {
		String email = "wrongpw@test.com";
		String oldPw = "OldPass123!";
		Member member = Member.builder()
			.email(email)
			.password(passwordEncoder.encode(oldPw))
			.name("비번실패")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build();
		memberRepository.save(member);

		String accessToken = getAccessToken(email, oldPw);

		// 틀린 현재 비밀번호로 요청
		PasswordChangeReq request = buildPasswordChangeRequest("WrongPass123!", "NewPass123!");

		mockMvc.perform(patch("/api/mypage/password").header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(MemberErrorCode.PASSWORD_MISMATCH.getStatus().value()))
			.andExpect(jsonPath("$.message").value(MemberErrorCode.PASSWORD_MISMATCH.getMessage()));
	}

	@Test
	@DisplayName("통합: 비밀번호 변경 실패 - 기존 비밀번호와 동일")
	void changePassword_fail_same() throws Exception {
		String email = "samepw@test.com";
		String oldPw = "SamePass123!";
		Member member = Member.builder()
			.email(email)
			.password(passwordEncoder.encode(oldPw))
			.name("동일비번")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build();
		memberRepository.save(member);

		String accessToken = getAccessToken(email, oldPw);

		// 현재 비번과 새 비번을 똑같이 요청
		PasswordChangeReq request = buildPasswordChangeRequest(oldPw, oldPw);

		mockMvc.perform(patch("/api/mypage/password").header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(MemberErrorCode.SAME_PASSWORD.getStatus().value()))
			.andExpect(jsonPath("$.message").value(MemberErrorCode.SAME_PASSWORD.getMessage()));
	}

	@Test
	@DisplayName("통합: 환불 계좌 등록 및 조회 성공")
	void refundAccount_integration_success() throws Exception {
		String email = "account@test.com";
		String password = "Password123!";
		Member member = Member.builder()
			.email(email)
			.password(passwordEncoder.encode(password))
			.name("계좌주인")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build();
		memberRepository.save(member);

		String accessToken = getAccessToken(email, password);

		// 계좌 등록 요청
		RefundAccountReq request = buildRefundAccountRequest(BankCode.WOORI, "100212345678", "계좌주인");

		// 생성
		mockMvc.perform(post("/api/mypage/account").header("Authorization", "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request))).andDo(print()).andExpect(status().isOk());

		// 조회
		mockMvc.perform(get("/api/mypage/account").header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.bankCode").value(request.bankCode().getCode()))
			.andExpect(jsonPath("$.data.bankName").value(request.bankCode().getDescription()))
			.andExpect(jsonPath("$.data.accountNumber").value(request.accountNumber()));
	}

	@Test
	@DisplayName("통합: 환불 계좌 수정 성공")
	void refundAccount_update_integration_success() throws Exception {
		String email = "update_acc@test.com";
		String password = "Password123!";
		Member member = Member.builder()
			.email(email)
			.password(passwordEncoder.encode(password))
			.name("수정맨")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build();
		memberRepository.save(member);
		String accessToken = getAccessToken(email, password);

		RefundAccountReq initRequest = buildRefundAccountRequest(BankCode.K_BANK, "1111111", "수정맨");

		mockMvc.perform(post("/api/mypage/account").header("Authorization", "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(initRequest)));

		// 계좌 수정 요청
		RefundAccountReq updateRequest = buildRefundAccountRequest(BankCode.CITY, "2222222", "수정맨");

		mockMvc.perform(post("/api/mypage/account").header("Authorization", "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(updateRequest))).andDo(print()).andExpect(status().isOk());

		mockMvc.perform(get("/api/mypage/account").header("Authorization", "Bearer " + accessToken))
			.andExpect(jsonPath("$.data.bankCode").value(updateRequest.bankCode().getCode()))
			.andExpect(jsonPath("$.data.bankName").value(updateRequest.bankCode().getDescription()))
			.andExpect(jsonPath("$.data.accountNumber").value(updateRequest.accountNumber()));
	}

	@Test
	@DisplayName("통합: 환불 계좌 등록 시 BankCode Enum 매핑 및 저장 확인")
	void saveRefundAccount_withBankCode() throws Exception {
		String email = "bankcode@test.com";
		String password = "Password123!";
		Member member = Member.builder()
			.email(email)
			.password(passwordEncoder.encode(password))
			.name("뱅크테스터")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build();
		memberRepository.save(member);

		String accessToken = getAccessToken(email, password);

		// 계좌 등록 요청
		String requestJson = """
			    {
			        "bankCode": "004",
			        "accountNumber": "123456789",
			        "holderName": "뱅크테스터"
			    }
			""";

		mockMvc.perform(post("/api/mypage/account").header("Authorization", "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content(requestJson)).andDo(print()).andExpect(status().isOk());

		// 조회 검증
		BankCode expectedBank = BankCode.KB;
		mockMvc.perform(get("/api/mypage/account").header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.bankCode").value(expectedBank.getCode()))
			.andExpect(jsonPath("$.data.bankName").value(expectedBank.getDescription()))
			.andExpect(jsonPath("$.data.accountNumber").value("123456789"));
	}

	// 로그인 및 AccessToken 추출 헬퍼 메서드
	private String getAccessToken(String email, String password) throws Exception {
		LoginReq loginRequest = buildLoginRequest(email, password);

		String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(loginRequest))).andReturn().getResponse().getContentAsString();

		JsonNode jsonNode = objectMapper.readTree(response);
		return jsonNode.path("data").path("accessToken").asText();
	}

	private PasswordChangeReq buildPasswordChangeRequest(String oldPassword, String newPassword) throws Exception {
		return new PasswordChangeReq(oldPassword, newPassword);
	}

	private RefundAccountReq buildRefundAccountRequest(BankCode bankCode, String accountNumber,
		String holderName) throws Exception {
		return new RefundAccountReq(bankCode, accountNumber, holderName);
	}

	private Member createMemberForIntegration(String email, String rawPassword) {
		return Member.builder()
			.email(email)
			.password(passwordEncoder.encode(rawPassword))
			.name("테스트유저")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build();
	}

	@Test
	@DisplayName("통합: 회원 탈퇴 성공")
	void withdraw_success() throws Exception {
		String email = "withdraw@test.com";
		String password = "Password123!";
		Member member = createMemberForIntegration(email, password);
		memberRepository.save(member);

		String accessToken = getAccessToken(email, password);
		String requestBody = "{\"password\": \"" + password + "\"}";

		mockMvc.perform(delete("/api/mypage/withdraw")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200));

		// DB 검증
		Member deleted = memberRepository.findByEmail(email).orElseThrow();
		if (!deleted.isDeleted()) {
			throw new IllegalStateException("회원 탈퇴가 정상 처리되지 않았습니다.");
		}
	}

	@Test
	@DisplayName("통합: 회원 탈퇴 실패 - 비밀번호 불일치")
	void withdraw_fail_wrongPassword() throws Exception {
		String email = "withdraw_fail@test.com";
		String password = "Password123!";
		Member member = createMemberForIntegration(email, password);
		memberRepository.save(member);

		String accessToken = getAccessToken(email, password);
		String requestBody = "{\"password\": \"WrongPass123!\"}";

		mockMvc.perform(delete("/api/mypage/withdraw")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value(MemberErrorCode.PASSWORD_MISMATCH.getMessage()));
	}

}
