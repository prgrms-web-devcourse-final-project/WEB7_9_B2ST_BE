package com.back.b2st.domain.auth.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.auth.client.KakaoApiClient;
import com.back.b2st.domain.auth.client.KakaoJwksClient;
import com.back.b2st.domain.auth.dto.oauth.KakaoIdTokenPayload;
import com.back.b2st.domain.auth.dto.request.KakaoLoginReq;
import com.back.b2st.domain.auth.dto.request.LoginReq;
import com.back.b2st.domain.auth.repository.RefreshTokenRepository;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.global.test.AbstractContainerBaseTest;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("카카오 로그인 통합 테스트")
class KakaoAuthControllerTest extends AbstractContainerBaseTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private KakaoApiClient kakaoApiClient;

	@MockitoBean
	private KakaoJwksClient kakaoJwksClient;

	@BeforeEach
	void setup() {
		refreshTokenRepository.deleteAll();
	}

	private Member createMember(String email, String password) {
		Member member = Member.builder()
			.email(email)
			.password(passwordEncoder.encode(password))
			.name("테스트유저")
			.phone("01012345678")
			.birth(LocalDate.of(1990, 1, 1))
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build();
		return memberRepository.save(member);
	}

	private Member createKakaoMember(String email, String kakaoId, String nickname) {
		Member member = Member.builder()
			.email(email)
			.password(null)
			.name(nickname)
			.phone(null)
			.birth(null)
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.KAKAO)
			.providerId(kakaoId)
			.isEmailVerified(true)
			.isIdentityVerified(false)
			.build();
		return memberRepository.save(member);
	}

	private KakaoIdTokenPayload createKakaoIdTokenPayload(Long kakaoId, String email, String nickname) {
		return new KakaoIdTokenPayload(
			"https://kauth.kakao.com", // iss
			"test-client-id", // aud
			String.valueOf(kakaoId), // sub
			System.currentTimeMillis() / 1000, // iat
			(System.currentTimeMillis() / 1000) + 3600, // exp
			System.currentTimeMillis() / 1000, // auth_time
			null, // nonce
			nickname,
			null, // picture
			email);
	}

	// 헬퍼 메서드

	@Nested
	@DisplayName("POST /api/auth/kakao - 카카오 로그인")
	class KakaoLoginApiTest {

		@Test
		@DisplayName("성공 - 신규 회원 가입 및 토큰 발급")
		void success_newMember() throws Exception {
			// given
			String code = "test-authorization-code";
			KakaoLoginReq request = new KakaoLoginReq(code, null);

			KakaoIdTokenPayload payload = createKakaoIdTokenPayload(
				123456789L, "newuser@kakao.com", "새로운유저");

			given(kakaoApiClient.getTokenAndParseIdToken(code)).willReturn(payload);

			// when & then
			MvcResult result = mockMvc.perform(post("/api/auth/kakao")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").exists())
				.andExpect(cookie().exists("refreshToken"))
				.andExpect(cookie().httpOnly("refreshToken", true))
				.andReturn();

			// 회원 생성 확인
			Member savedMember = memberRepository.findByEmail("newuser@kakao.com").orElse(null);
			assertThat(savedMember).isNotNull();
			assertThat(savedMember.getProvider()).isEqualTo(Member.Provider.KAKAO);
			assertThat(savedMember.getProviderId()).isEqualTo("123456789");
			assertThat(savedMember.getName()).isEqualTo("새로운유저");
			assertThat(savedMember.isEmailVerified()).isTrue();

			// Redis 토큰 저장 확인
			assertThat(refreshTokenRepository.findById("newuser@kakao.com")).isPresent();
		}

		@Test
		@DisplayName("성공 - 기존 카카오 회원 로그인")
		void success_existingKakaoMember() throws Exception {
			// given
			String email = "existing@kakao.com";
			String kakaoId = "987654321";
			createKakaoMember(email, kakaoId, "기존유저");

			String code = "test-code";
			KakaoLoginReq request = new KakaoLoginReq(code, null);

			KakaoIdTokenPayload payload = createKakaoIdTokenPayload(
				987654321L, email, "기존유저");

			given(kakaoApiClient.getTokenAndParseIdToken(code)).willReturn(payload);

			// when & then
			mockMvc.perform(post("/api/auth/kakao")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").exists());

			// 회원 수 변경 없음 확인 (신규 생성 안 됨)
			long count = memberRepository.count();
			assertThat(count).isEqualTo(1);
		}

		@Test
		@DisplayName("성공 - 이메일 가입 회원에 카카오 자동 연동")
		void success_autoLinkToEmailMember() throws Exception {
			// given
			String email = "emailuser@test.com";
			createMember(email, "Password123!");

			String code = "test-code";
			KakaoLoginReq request = new KakaoLoginReq(code, null);

			KakaoIdTokenPayload payload = createKakaoIdTokenPayload(
				111222333L, email, "이메일유저");

			given(kakaoApiClient.getTokenAndParseIdToken(code)).willReturn(payload);

			// when & then
			mockMvc.perform(post("/api/auth/kakao")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").exists());

			// 카카오 연동 확인
			Member updatedMember = memberRepository.findByEmail(email).orElseThrow();
			assertThat(updatedMember.getProviderId()).isEqualTo("111222333");
		}

		@Test
		@DisplayName("실패 - 인가 코드 누락")
		void fail_missingCode() throws Exception {
			// given
			String requestJson = "{}";

			// when & then
			mockMvc.perform(post("/api/auth/kakao")
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("실패 - 이메일 미제공 (동의 안 함)")
		void fail_emailNotProvided() throws Exception {
			// given
			String code = "test-code";
			KakaoLoginReq request = new KakaoLoginReq(code, null);

			KakaoIdTokenPayload payload = createKakaoIdTokenPayload(
				123456789L, null, "유저" // 이메일 없음
			);

			given(kakaoApiClient.getTokenAndParseIdToken(code)).willReturn(payload);

			// when & then
			mockMvc.perform(post("/api/auth/kakao")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("이메일 정보 제공에 동의해주세요."));
		}

		@Test
		@DisplayName("실패 - 탈퇴 회원")
		void fail_withdrawnMember() throws Exception {
			// given
			String email = "withdrawn@kakao.com";
			String kakaoId = "999888777";
			Member member = createKakaoMember(email, kakaoId, "탈퇴유저");
			member.softDelete();
			memberRepository.save(member);

			String code = "test-code";
			KakaoLoginReq request = new KakaoLoginReq(code, null);

			KakaoIdTokenPayload payload = createKakaoIdTokenPayload(
				999888777L, email, "탈퇴유저");

			given(kakaoApiClient.getTokenAndParseIdToken(code)).willReturn(payload);

			// when & then
			mockMvc.perform(post("/api/auth/kakao")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("이미 탈퇴한 회원입니다."));
		}
	}

	@Nested
	@DisplayName("GET /api/auth/kakao/callback - 콜백 테스트")
	class KakaoCallbackApiTest {

		@Test
		@DisplayName("성공 - 콜백 URL로 로그인")
		void success() throws Exception {
			// given
			String code = "callback-test-code";

			KakaoIdTokenPayload payload = createKakaoIdTokenPayload(
				456789123L, "callback@kakao.com", "콜백유저");

			given(kakaoApiClient.getTokenAndParseIdToken(code)).willReturn(payload);

			// when & then
			mockMvc.perform(get("/api/auth/kakao/callback")
					.param("code", code))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").exists());
		}
	}

	@Nested
	@DisplayName("POST /api/auth/link/kakao - 카카오 계정 연동")
	class LinkKakaoApiTest {

		@Test
		@DisplayName("성공 - 로그인한 회원에 카카오 연동")
		void success() throws Exception {
			// given
			String email = "linkuser@test.com";
			String password = "Password123!";
			createMember(email, password);

			// 먼저 로그인
			LoginReq loginReq = new LoginReq(email, password);
			String loginResponse = mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(loginReq)))
				.andReturn().getResponse().getContentAsString();

			String accessToken = objectMapper.readTree(loginResponse)
				.path("data").path("accessToken").stringValue();

			// 카카오 API Mock
			String code = "link-code";
			KakaoIdTokenPayload payload = createKakaoIdTokenPayload(
				777888999L, "other@kakao.com", "연동유저");
			given(kakaoApiClient.getTokenAndParseIdToken(code)).willReturn(payload);

			KakaoLoginReq linkRequest = new KakaoLoginReq(code, null);

			// when & then
			mockMvc.perform(post("/api/auth/link/kakao")
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(linkRequest)))
				.andDo(print())
				.andExpect(status().isOk());

			// 연동 확인
			Member updatedMember = memberRepository.findByEmail(email).orElseThrow();
			assertThat(updatedMember.getProviderId()).isEqualTo("777888999");
		}

		@Test
		@DisplayName("실패 - 인증 없이 연동 시도")
		void fail_unauthorized() throws Exception {
			// given
			KakaoLoginReq request = new KakaoLoginReq("test-code", null);

			// when & then
			mockMvc.perform(post("/api/auth/link/kakao")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isUnauthorized());
		}
	}
}
