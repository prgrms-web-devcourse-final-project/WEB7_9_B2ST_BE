package com.back.b2st.domain.member.controller;

import static com.back.b2st.domain.auth.service.AuthTestRequestBuilder.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

	@Nested
	@DisplayName("내 정보 조회 API")
	class GetMyInfoTest {

		@Test
		@DisplayName("성공")
		void success() throws Exception {
			String email = "mypage@test.com";
			Member member = createMember(email, "Password123!", "마이페이지");
			memberRepository.save(member);

			String accessToken = getAccessToken(email, "Password123!");

			mockMvc.perform(get("/api/mypage/me")
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.email").value(email))
				.andExpect(jsonPath("$.data.name").value(member.getName()));
		}

		@Test
		@DisplayName("실패 - 토큰 없음")
		void fail_noToken() throws Exception {
			mockMvc.perform(get("/api/mypage/me").contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value(401))
				.andExpect(jsonPath("$.message").value(CommonErrorCode.UNAUTHORIZED.getMessage()));
		}
	}

	@Nested
	@DisplayName("비밀번호 변경 API")
	class ChangePasswordTest {

		@Test
		@DisplayName("성공")
		void success() throws Exception {
			String email = "pwchange@test.com";
			String oldPw = "OldPass123!";
			Member member = createMember(email, oldPw, "비번변경");
			memberRepository.save(member);

			String accessToken = getAccessToken(email, oldPw);
			PasswordChangeReq request = new PasswordChangeReq(oldPw, "NewPass123!");

			mockMvc.perform(patch("/api/mypage/password")
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk());

			Member updatedMember = memberRepository.findByEmail(email).orElseThrow();
			if (!passwordEncoder.matches("NewPass123!", updatedMember.getPassword())) {
				throw new IllegalStateException("비밀번호가 변경되지 않았습니다.");
			}
		}

		@Test
		@DisplayName("실패 - 현재 비밀번호 불일치")
		void fail_mismatch() throws Exception {
			String email = "wrongpw@test.com";
			String oldPw = "OldPass123!";
			Member member = createMember(email, oldPw, "비번실패");
			memberRepository.save(member);

			String accessToken = getAccessToken(email, oldPw);
			PasswordChangeReq request = new PasswordChangeReq("WrongPass123!", "NewPass123!");

			mockMvc.perform(patch("/api/mypage/password")
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(MemberErrorCode.PASSWORD_MISMATCH.getStatus().value()));
		}

		@Test
		@DisplayName("실패 - 기존 비밀번호와 동일")
		void fail_samePassword() throws Exception {
			String email = "samepw@test.com";
			String oldPw = "SamePass123!";
			Member member = createMember(email, oldPw, "동일비번");
			memberRepository.save(member);

			String accessToken = getAccessToken(email, oldPw);
			PasswordChangeReq request = new PasswordChangeReq(oldPw, oldPw);

			mockMvc.perform(patch("/api/mypage/password")
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(MemberErrorCode.SAME_PASSWORD.getStatus().value()));
		}
	}

	@Nested
	@DisplayName("환불 계좌 API")
	class RefundAccountTest {

		@Test
		@DisplayName("등록 및 조회 성공")
		void saveAndGet_success() throws Exception {
			String email = "account@test.com";
			String password = "Password123!";
			Member member = createMember(email, password, "계좌주인");
			memberRepository.save(member);

			String accessToken = getAccessToken(email, password);
			RefundAccountReq request = new RefundAccountReq(BankCode.WOORI, "100212345678", "계좌주인");

			mockMvc.perform(post("/api/mypage/account")
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk());

			mockMvc.perform(get("/api/mypage/account")
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.bankCode").value(request.bankCode().getCode()))
				.andExpect(jsonPath("$.data.accountNumber").value(request.accountNumber()));
		}

		@Test
		@DisplayName("수정 성공")
		void update_success() throws Exception {
			String email = "update_acc@test.com";
			String password = "Password123!";
			Member member = createMember(email, password, "수정맨");
			memberRepository.save(member);

			String accessToken = getAccessToken(email, password);
			RefundAccountReq initRequest = new RefundAccountReq(BankCode.K_BANK, "1111111", "수정맨");

			mockMvc.perform(post("/api/mypage/account")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(initRequest)));

			RefundAccountReq updateRequest = new RefundAccountReq(BankCode.CITY, "2222222", "수정맨");

			mockMvc.perform(post("/api/mypage/account")
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(updateRequest)))
				.andDo(print())
				.andExpect(status().isOk());

			mockMvc.perform(get("/api/mypage/account").header("Authorization", "Bearer " + accessToken))
				.andExpect(jsonPath("$.data.bankCode").value(updateRequest.bankCode().getCode()))
				.andExpect(jsonPath("$.data.accountNumber").value(updateRequest.accountNumber()));
		}

		@Test
		@DisplayName("BankCode Enum 매핑 확인")
		void bankCodeMapping() throws Exception {
			String email = "bankcode@test.com";
			String password = "Password123!";
			Member member = createMember(email, password, "뱅크테스터");
			memberRepository.save(member);

			String accessToken = getAccessToken(email, password);
			String requestJson = """
				{
				    "bankCode": "004",
				    "accountNumber": "123456789",
				    "holderName": "뱅크테스터"
				}
				""";

			mockMvc.perform(post("/api/mypage/account")
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson))
				.andDo(print())
				.andExpect(status().isOk());

			BankCode expectedBank = BankCode.KB;
			mockMvc.perform(get("/api/mypage/account")
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.bankCode").value(expectedBank.getCode()))
				.andExpect(jsonPath("$.data.bankName").value(expectedBank.getDescription()));
		}
	}

	@Nested
	@DisplayName("회원 탈퇴 API")
	class WithdrawTest {

		@Test
		@DisplayName("성공")
		void success() throws Exception {
			String email = "withdraw@test.com";
			String password = "Password123!";
			Member member = createMember(email, password, "테스트유저");
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

			Member deleted = memberRepository.findByEmail(email).orElseThrow();
			if (!deleted.isDeleted()) {
				throw new IllegalStateException("회원 탈퇴가 정상 처리되지 않았습니다.");
			}
		}

		@Test
		@DisplayName("실패 - 비밀번호 불일치")
		void fail_wrongPassword() throws Exception {
			String email = "withdraw_fail@test.com";
			String password = "Password123!";
			Member member = createMember(email, password, "테스트유저");
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

	// 헬퍼 메서드
	private Member createMember(String email, String password, String name) {
		return Member.builder()
			.email(email)
			.password(passwordEncoder.encode(password))
			.name(name)
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build();
	}

	private String getAccessToken(String email, String password) throws Exception {
		LoginReq loginRequest = buildLoginRequest(email, password);

		String response = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andReturn().getResponse().getContentAsString();

		JsonNode jsonNode = objectMapper.readTree(response);
		return jsonNode.path("data").path("accessToken").asText();
	}
}
