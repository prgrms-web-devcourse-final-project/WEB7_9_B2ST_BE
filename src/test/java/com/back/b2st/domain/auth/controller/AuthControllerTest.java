package com.back.b2st.domain.auth.controller;

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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.auth.dto.LoginRequest;
import com.back.b2st.domain.auth.dto.TokenReissueRequest;
import com.back.b2st.domain.auth.entity.RefreshToken;
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
	// 컨테이너 설정 상속받음
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
		// 테스트마다 Redis 비우기
		refreshTokenRepository.deleteAll();
	}

	@Test
	@DisplayName("통합: 로그인 성공 후 AccessToken 반환 및 Redis 저장 확인")
	void login_integration_success() throws Exception {
		// 회원 미리 생성 (H2 DB)
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

		// 로그인 요청 객체 생성
		LoginRequest request = new LoginRequest();
		ReflectionTestUtils.setField(request, "email", email);
		ReflectionTestUtils.setField(request, "password", password);

		// API 요청
		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessToken").exists())
			.andExpect(jsonPath("$.data.refreshToken").exists());

		// Redis 검증: 실제 RefreshToken이 저장되었는지 확인
		// RefreshToken 엔티티의 @Id는 email이므로 findById(email)로 조회
		RefreshToken savedToken = refreshTokenRepository.findById(email).orElse(null);

		assertThat(savedToken).isNotNull();
		assertThat(savedToken.getToken()).isNotEmpty();
		System.out.println(">>> Redis Saved Token: " + savedToken.getToken());
	}

	@Test
	@DisplayName("통합: 로그인 실패 - 비밀번호 불일치")
	void login_integration_fail_password() throws Exception {
		// 회원 생성
		Member member = Member.builder()
			.email("fail@test.com")
			.password(passwordEncoder.encode("Password123!"))
			.name("유저")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.build();
		memberRepository.save(member);

		// 틀린 비밀번호 요청
		LoginRequest request = new LoginRequest();
		ReflectionTestUtils.setField(request, "email", "fail@test.com");
		ReflectionTestUtils.setField(request, "password", "WrongPw123!");

		// 요청 및 401/500 에러 확인
		// Spring Security 기본 설정상 인증 실패는 401.
		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isUnauthorized()); // 인증 실패
	}

	@Test
	@DisplayName("통합: 토큰 재발급 성공")
	void reissue_integration_success() throws Exception {
		String email = "reissue@test.com";

		// 회원 가입
		Member member = Member.builder()
			.email(email)
			.password(passwordEncoder.encode("Password123!"))
			.name("유저")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.build();
		memberRepository.save(member);

		// 로그인 요청
		LoginRequest loginRequest = new LoginRequest();
		ReflectionTestUtils.setField(loginRequest, "email", email);
		ReflectionTestUtils.setField(loginRequest, "password", "Password123!");

		String responseBody = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(loginRequest))).andReturn().getResponse().getContentAsString();

		JsonNode rootNode = objectMapper.readTree(responseBody);
		JsonNode dataNode = rootNode.path("data");

		String accessToken = dataNode.path("accessToken").asText();
		String refreshToken = dataNode.path("refreshToken").asText();

		Thread.sleep(1500);

		// 재발급 요청 객체 생성
		TokenReissueRequest reissueRequest = TokenReissueRequest.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.build();

		// API 실행 및 검증
		mockMvc.perform(post("/auth/reissue").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(reissueRequest)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessToken").exists())
			.andExpect(jsonPath("$.data.accessToken").isString());

		// Redis 값 변경 확인
		RefreshToken updatedRedisToken = refreshTokenRepository.findById(email).orElseThrow();
		assertThat(updatedRedisToken.getToken()).isNotEqualTo(refreshToken);
	}

	@Test
	@DisplayName("통합: 로그아웃 성공")
	void logout_integration_success() throws Exception {
		// 로그인 상태 가정
		String email = "logout@test.com";
		refreshTokenRepository.save(new RefreshToken(email, "someRefreshToken"));

		// 로그인 토큰 발급
		Member member = Member.builder()
			.email(email)
			.password(passwordEncoder.encode("Password123!"))
			.name("로그아웃")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.build();
		memberRepository.save(member);

		LoginRequest loginRequest = new LoginRequest();
		ReflectionTestUtils.setField(loginRequest, "email", email);
		ReflectionTestUtils.setField(loginRequest, "password", "Password123!");

		String loginResponse = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andReturn().getResponse().getContentAsString();

		String accessToken = objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();

		// 로그아웃 요청
		mockMvc.perform(post("/auth/logout")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk());

		boolean exists = refreshTokenRepository.findById(email).isPresent();
		assertThat(exists).isFalse();
	}
}
