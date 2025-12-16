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
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.auth.dto.request.LoginReq;
import com.back.b2st.domain.auth.dto.request.TokenReissueReq;
import com.back.b2st.domain.auth.entity.RefreshToken;
import com.back.b2st.domain.auth.error.AuthErrorCode;
import com.back.b2st.domain.auth.repository.RefreshTokenRepository;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.global.test.AbstractContainerBaseTest;

import tools.jackson.databind.JsonNode;
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
		Member member = Member.builder()
			.email(email)
			.password(passwordEncoder.encode(password))
			.name("로그인유저")
			.role(Member.Role.MEMBER)
			.isVerified(true)
			.provider(Member.Provider.EMAIL)
			.build();
		memberRepository.save(member);

		LoginReq request = buildLoginRequest(email, password);

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessToken").exists())
			.andExpect(jsonPath("$.data.refreshToken").exists());

		RefreshToken savedToken = refreshTokenRepository.findById(email).orElse(null);
		assertThat(savedToken).isNotNull();
		assertThat(savedToken.getToken()).isNotEmpty();
	}

	@Test
	@DisplayName("통합: 로그인 실패 - 비밀번호 불일치")
	void login_integration_fail_password() throws Exception {
		// given
		Member member = Member.builder()
			.email("fail@test.com")
			.password(passwordEncoder.encode("Password123!"))
			.name("유저")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.build();
		memberRepository.save(member);

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

		Member member = Member.builder()
			.email(email)
			.password(passwordEncoder.encode("Password123!"))
			.name("유저")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.build();
		memberRepository.save(member);

		LoginReq loginRequest = buildLoginRequest(email, "Password123!");

		String responseBody = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(loginRequest))).andReturn().getResponse().getContentAsString();

		JsonNode rootNode = objectMapper.readTree(responseBody);
		JsonNode dataNode = rootNode.path("data");

		String accessToken = dataNode.path("accessToken").asText();
		String refreshToken = dataNode.path("refreshToken").asText();

		Thread.sleep(1500);

		TokenReissueReq reissueRequest = new TokenReissueReq(accessToken, refreshToken);

		mockMvc.perform(post("/api/auth/reissue").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(reissueRequest)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessToken").exists())
			.andExpect(jsonPath("$.data.accessToken").isString());

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
		refreshTokenRepository.save(new RefreshToken(email, "someRefreshToken"));

		Member member = Member.builder()
			.email(email)
			.password(passwordEncoder.encode("Password123!"))
			.name("로그아웃")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.build();
		memberRepository.save(member);

		LoginReq loginRequest = buildLoginRequest(email, "Password123!");

		String loginResponse = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andReturn().getResponse().getContentAsString();

		String accessToken = objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();

		mockMvc.perform(post("/api/auth/logout")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk());

		boolean exists = refreshTokenRepository.findById(email).isPresent();
		assertThat(exists).isFalse();
	}
}
