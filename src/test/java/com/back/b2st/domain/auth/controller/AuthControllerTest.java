package com.back.b2st.domain.auth.controller;

import static com.back.b2st.domain.auth.service.AuthTestRequestBuilder.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.UUID;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.auth.client.KakaoJwksClient;
import com.back.b2st.domain.auth.dto.request.ConfirmRecoveryReq;
import com.back.b2st.domain.auth.dto.request.LoginReq;
import com.back.b2st.domain.auth.dto.request.RecoveryEmailReq;
import com.back.b2st.domain.auth.dto.request.TokenReissueReq;
import com.back.b2st.domain.auth.entity.RefreshToken;
import com.back.b2st.domain.auth.entity.WithdrawalRecoveryToken;
import com.back.b2st.domain.auth.error.AuthErrorCode;
import com.back.b2st.domain.auth.repository.RefreshTokenRepository;
import com.back.b2st.domain.auth.repository.WithdrawalRecoveryRepository;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.global.test.AbstractContainerBaseTest;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest extends AbstractContainerBaseTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private WithdrawalRecoveryRepository recoveryRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	@org.springframework.test.context.bean.override.mockito.MockitoBean
	private KakaoJwksClient kakaoJwksClient;

	@BeforeEach
	void setup() {
		refreshTokenRepository.deleteAll();
		recoveryRepository.deleteAll();
	}

	// 헬퍼 메서드
	private Member createMember(String email, String password) {
		Member member = Member.builder()
			.email(email)
			.password(passwordEncoder.encode(password))
			.name("유저일")
			.phone("01012345678")
			.birth(LocalDate.of(1990, 1, 1))
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build();
		return memberRepository.save(member);
	}

	private Member createWithdrawnMember(String email, String password) {
		Member member = createMember(email, password);
		member.softDelete();
		return memberRepository.save(member);
	}

	@Nested
	@DisplayName("로그인 API")
	class LoginTest {

		@Test
		@DisplayName("성공")
		void success() throws Exception {
			String email = "login@test.com";
			String password = "Password123!";
			createMember(email, password);

			LoginReq request = buildLoginRequest(email, password);

			mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").exists())
				.andExpect(cookie().exists("refreshToken"))
				.andExpect(cookie().httpOnly("refreshToken", true));

			RefreshToken savedToken = refreshTokenRepository.findById(email).orElse(null);
			assertThat(savedToken).isNotNull();
		}

		@Test
		@DisplayName("실패 - 비밀번호 불일치")
		void fail_wrongPassword() throws Exception {
			createMember("fail@test.com", "Password123!");

			LoginReq request = buildLoginRequest("fail@test.com", "WrongPw123!");

			mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value(AuthErrorCode.LOGIN_FAILED.getStatus().value()));
		}
	}

	@Nested
	@DisplayName("토큰 재발급 API")
	class ReissueTest {

		@Test
		@DisplayName("성공")
		void success() throws Exception {
			String email = "reissue@test.com";
			String password = "Password123!";
			createMember(email, password);

			LoginReq loginReq = new LoginReq(email, password);
			MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(loginReq)))
				.andReturn();

			String responseBody = loginResult.getResponse().getContentAsString();
			String accessToken = objectMapper.readTree(responseBody).path("data").path("accessToken").asString();

			Cookie refreshCookie = loginResult.getResponse().getCookie("refreshToken");
			assertThat(refreshCookie).isNotNull();
			String refreshToken = refreshCookie.getValue();

			Thread.sleep(1500);

			TokenReissueReq reissueRequest = new TokenReissueReq(accessToken, null);

			mockMvc.perform(post("/api/auth/reissue")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(reissueRequest))
					.cookie(refreshCookie))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").exists());

			RefreshToken updatedRedisToken = refreshTokenRepository.findById(email).orElseThrow();
			assertThat(updatedRedisToken.getToken()).isNotEqualTo(refreshToken);
		}

		@Test
		@DisplayName("실패 - 유효하지 않은 Refresh Token")
		void fail_invalidToken() throws Exception {
			TokenReissueReq reissueRequest = new TokenReissueReq("dummy_access_token", "invalid_refresh_token_format");

			mockMvc.perform(post("/api/auth/reissue")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(reissueRequest)))
				.andDo(print())
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_TOKEN.getStatus().value()));
		}
	}

	@Nested
	@DisplayName("로그아웃 API")
	class LogoutTest {

		@Test
		@DisplayName("성공")
		void success() throws Exception {
			String email = "logout@test.com";
			String password = "Password123!";
			createMember(email, password);

			LoginReq loginReq = new LoginReq(email, password);
			String loginResponse = mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(loginReq)))
				.andReturn().getResponse().getContentAsString();

			String accessToken = objectMapper.readTree(loginResponse).path("data").path("accessToken").asString();

			mockMvc.perform(post("/api/auth/logout")
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk());

			boolean exists = refreshTokenRepository.findById(email).isPresent();
			assertThat(exists).isFalse();
		}
	}

	@Nested
	@DisplayName("탈퇴 복구 이메일 발송 API")
	class WithdrawalRecoveryTest {

		@Test
		@DisplayName("성공")
		void success() throws Exception {
			// given
			Member member = createWithdrawnMember("withdrawn@test.com", "Password123!");

			RecoveryEmailReq request = new RecoveryEmailReq(member.getEmail());

			// when & then
			mockMvc.perform(post("/api/auth/withdrawal-recovery")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk());
		}

		@Test
		@DisplayName("실패 - 존재하지 않는 이메일")
		void fail_memberNotFound() throws Exception {
			RecoveryEmailReq request = new RecoveryEmailReq("notfound@test.com");

			mockMvc.perform(post("/api/auth/withdrawal-recovery")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isNotFound());
		}

		@Test
		@DisplayName("실패 - 탈퇴 상태 아님")
		void fail_notWithdrawn() throws Exception {
			createMember("active@test.com", "Password123!");

			RecoveryEmailReq request = new RecoveryEmailReq("active@test.com");

			mockMvc.perform(post("/api/auth/withdrawal-recovery")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("복구 확인 API")
	class ConfirmRecoveryTest {

		@Test
		@DisplayName("성공")
		void success() throws Exception {
			// given
			Member member = createWithdrawnMember("recovery@test.com", "Password123!");

			String token = UUID.randomUUID().toString();
			WithdrawalRecoveryToken recoveryToken = WithdrawalRecoveryToken.builder()
				.token(token)
				.email(member.getEmail())
				.memberId(member.getId())
				.build();
			recoveryRepository.save(recoveryToken);

			ConfirmRecoveryReq request = new ConfirmRecoveryReq(token);

			// when & then
			mockMvc.perform(post("/api/auth/confirm-recovery")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk());

			// 회원 복구 확인
			Member updatedMember = memberRepository.findByEmail(member.getEmail()).orElseThrow();
			assertThat(updatedMember.isDeleted()).isFalse();
		}

		@Test
		@DisplayName("실패 - 유효하지 않은 토큰")
		void fail_invalidToken() throws Exception {
			ConfirmRecoveryReq request = new ConfirmRecoveryReq("invalid-token");

			mockMvc.perform(post("/api/auth/confirm-recovery")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isNotFound());
		}
	}
}
