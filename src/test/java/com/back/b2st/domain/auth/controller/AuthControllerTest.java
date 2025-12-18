package com.back.b2st.domain.auth.controller;

import static com.back.b2st.domain.auth.service.AuthTestRequestBuilder.*;
import static org.assertj.core.api.Assertions.*;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.auth.dto.request.LoginReq;
import com.back.b2st.domain.auth.dto.request.TokenReissueReq;
import com.back.b2st.domain.auth.entity.RefreshToken;
import com.back.b2st.domain.auth.error.AuthErrorCode;
import com.back.b2st.domain.auth.repository.RefreshTokenRepository;
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
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void setup() {
		refreshTokenRepository.deleteAll();
	}

	@Test
	@DisplayName("통합: 로그인 성공 후 AccessToken 반환 및 Redis 저장 확인")
	void login_integration_success() throws Exception {
		String email = "login@test.com";
		String password = "Password123!";
		createMember(email, password);

		LoginReq request = buildLoginRequest(email, password);

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessToken").exists())
			.andExpect(jsonPath("$.data.refreshToken").doesNotExist())
			.andExpect(cookie().exists("refreshToken"))
			.andExpect(cookie().httpOnly("refreshToken", true));

		RefreshToken savedToken = refreshTokenRepository.findById(email).orElse(null);
		assertThat(savedToken).isNotNull();
	}

	@Test
	@DisplayName("통합: 로그인 실패 - 비밀번호 불일치")
	void login_integration_fail_password() throws Exception {
		// given
		createMember("fail@test.com", "Password123!");

		LoginReq request = buildLoginRequest("fail@test.com", "WrongPw123!");

		// when & then
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isUnauthorized()) // HTTP Header Status: 401
			.andExpect(jsonPath("$.code").value(AuthErrorCode.LOGIN_FAILED.getStatus().value()))
			.andExpect(jsonPath("$.message").value(AuthErrorCode.LOGIN_FAILED.getMessage()));
	}

	@Test
	@DisplayName("통합: 토큰 재발급 성공")
	void reissue_integration_success() throws Exception {
		String email = "reissue@test.com";
		String password = "Password123!";
		createMember(email, password);

		LoginReq loginReq = new LoginReq(email, password);
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginReq)))
			.andReturn();

		// 토큰 추출 Body -> AT, Cookie -> RT
		String responseBody = loginResult.getResponse().getContentAsString();
		String accessToken = objectMapper.readTree(responseBody).path("data").path("accessToken").asText();

		// 쿠키서 refresh token 꺼내기
		Cookie refreshCookie = loginResult.getResponse().getCookie("refreshToken");
		assertThat(refreshCookie).isNotNull();
		String refreshToken = refreshCookie.getValue();

		Thread.sleep(1500);

		TokenReissueReq reissueRequest = new TokenReissueReq(accessToken, null);

		// when & then
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
	@DisplayName("통합: 토큰 재발급 실패 - 유효하지 않은 Refresh Token")
	void reissue_integration_fail_invalid_token() throws Exception {
		// given
		TokenReissueReq reissueRequest = new TokenReissueReq("dummy_access_token", "invalid_refresh_token_format");

		// when & then
		mockMvc.perform(post("/api/auth/reissue")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(reissueRequest)))
			.andDo(print())
			.andExpect(status().isUnauthorized()) // HTTP Header Status: 401
			.andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_TOKEN.getStatus().value()))
			.andExpect(jsonPath("$.message").value(AuthErrorCode.INVALID_TOKEN.getMessage()));
	}

	@Test
	@DisplayName("통합: 로그아웃 성공")
	void logout_integration_success() throws Exception {
		String email = "logout@test.com";
		String password = "Password123!";
		createMember(email, password);

		LoginReq loginReq = new LoginReq(email, password);
		String loginResponse = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginReq)))
			.andReturn().getResponse().getContentAsString();

		String accessToken = objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();

		// when
		mockMvc.perform(post("/api/auth/logout")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk());

		// then
		boolean exists = refreshTokenRepository.findById(email).isPresent();
		assertThat(exists).isFalse();
	}

	private void createMember(String email, String password) {
		Member member = Member.builder()
			.email(email)
			.password(passwordEncoder.encode(password))
			.name("유저일")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isVerified(true)
			.build();
		memberRepository.save(member);
	}
}
