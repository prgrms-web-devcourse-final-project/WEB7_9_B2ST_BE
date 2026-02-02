package com.back.b2st.domain.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.auth.dto.request.LoginReq;
import com.back.b2st.domain.auth.entity.LoginLog;
import com.back.b2st.domain.auth.repository.LoginLogRepository;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.entity.SignupLog;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.member.repository.SignupLogRepository;
import com.back.b2st.global.test.AbstractContainerBaseTest;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthAdminControllerTest extends AbstractContainerBaseTest {

	private static final String LOCK_KEY_PREFIX = "login:lock:";
	private static final String ADMIN_PASSWORD = "AdminPass123!";
	private static final String MEMBER_PASSWORD = "MemberPass123!";
	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private LoginLogRepository loginLogRepository;
	@Autowired
	private SignupLogRepository signupLogRepository;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private StringRedisTemplate redisTemplate;
	private String adminToken;
	private String memberToken;

	@BeforeEach
	void setUp() throws Exception {
		// 관리자 회원 생성
		Member adminMember = Member.builder()
				.email("admin@test.com")
				.password(passwordEncoder.encode(ADMIN_PASSWORD))
				.name("관리자")
				.phone("01087654321")
				.birth(LocalDate.of(1985, 1, 1))
				.role(Member.Role.ADMIN)
				.provider(Member.Provider.EMAIL)
				.isEmailVerified(true)
				.isIdentityVerified(false)
				.build();
		memberRepository.save(adminMember);

		// 일반 회원 생성
		Member testMember = Member.builder()
				.email("member@test.com")
				.password(passwordEncoder.encode(MEMBER_PASSWORD))
				.name("테스트회원")
				.phone("01012345678")
				.birth(LocalDate.of(1990, 1, 1))
				.role(Member.Role.MEMBER)
				.provider(Member.Provider.EMAIL)
				.isEmailVerified(true)
				.isIdentityVerified(false)
				.build();
		memberRepository.save(testMember);

		// 테스트 로그인 로그 생성
		LoginLog successLog = LoginLog.builder()
				.email("user@test.com")
				.clientIp("192.168.1.1")
				.success(true)
				.attemptedAt(LocalDateTime.now())
				.build();
		loginLogRepository.save(successLog);

		LoginLog failLog = LoginLog.builder()
				.email("user@test.com")
				.clientIp("192.168.1.2")
				.success(false)
				.failReason(LoginLog.FailReason.INVALID_PASSWORD)
				.attemptedAt(LocalDateTime.now())
				.build();
		loginLogRepository.save(failLog);

		// 테스트 가입 로그 생성
		SignupLog signupLog = SignupLog.builder()
				.email("newuser@test.com")
				.clientIp("192.168.1.3")
				.build();
		signupLogRepository.save(signupLog);

		// JWT 토큰 발급
		adminToken = getAccessToken("admin@test.com", ADMIN_PASSWORD);
		memberToken = getAccessToken("member@test.com", MEMBER_PASSWORD);
	}

	/**
	 * 로그인 후 AccessToken 획득
	 */
	private String getAccessToken(String email, String password) throws Exception {
		LoginReq loginReq = new LoginReq(email, password);
		String response = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginReq)))
				.andReturn().getResponse().getContentAsString();

		return objectMapper.readTree(response).path("data").path("accessToken").asString();
	}

	@Nested
	@DisplayName("로그인 로그 조회 API")
	class GetLoginLogsTest {

		@Test
		@DisplayName("성공 - 관리자 권한")
		void success_withAdminRole() throws Exception {
			mockMvc.perform(get("/api/admin/auth/logs/login")
					.header("Authorization", "Bearer " + adminToken)
					.contentType(MediaType.APPLICATION_JSON))
					.andDo(print())
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value(200))
					.andExpect(jsonPath("$.data.content").isArray());
		}

		@Test
		@DisplayName("성공 - 이메일 필터")
		void success_filterByEmail() throws Exception {
			mockMvc.perform(get("/api/admin/auth/logs/login")
					.header("Authorization", "Bearer " + adminToken)
					.param("email", "user@test.com")
					.contentType(MediaType.APPLICATION_JSON))
					.andDo(print())
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value(200))
					.andExpect(jsonPath("$.data.content").isArray());
		}

		@Test
		@DisplayName("성공 - 실패 로그만 조회")
		void success_filterByFailure() throws Exception {
			mockMvc.perform(get("/api/admin/auth/logs/login")
					.header("Authorization", "Bearer " + adminToken)
					.param("success", "false")
					.contentType(MediaType.APPLICATION_JSON))
					.andDo(print())
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value(200));
		}

		@Test
		@DisplayName("실패 - 일반 회원 권한")
		void fail_withMemberRole() throws Exception {
			mockMvc.perform(get("/api/admin/auth/logs/login")
					.header("Authorization", "Bearer " + memberToken)
					.contentType(MediaType.APPLICATION_JSON))
					.andDo(print())
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	@DisplayName("가입 로그 조회 API")
	class GetSignupLogsTest {

		@Test
		@DisplayName("성공")
		void success() throws Exception {
			mockMvc.perform(get("/api/admin/auth/logs/signup")
					.header("Authorization", "Bearer " + adminToken)
					.contentType(MediaType.APPLICATION_JSON))
					.andDo(print())
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value(200))
					.andExpect(jsonPath("$.data.content").isArray());
		}

		@Test
		@DisplayName("성공 - 시간 범위 지정")
		void success_withHours() throws Exception {
			mockMvc.perform(get("/api/admin/auth/logs/signup")
					.header("Authorization", "Bearer " + adminToken)
					.param("hours", "1")
					.contentType(MediaType.APPLICATION_JSON))
					.andDo(print())
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value(200));
		}

		@Test
		@DisplayName("실패 - 일반 회원 권한")
		void fail_withMemberRole() throws Exception {
			mockMvc.perform(get("/api/admin/auth/logs/signup")
					.header("Authorization", "Bearer " + memberToken)
					.contentType(MediaType.APPLICATION_JSON))
					.andDo(print())
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	@DisplayName("잠긴 계정 목록 조회 API")
	class GetLockedAccountsTest {

		@Test
		@DisplayName("성공 - 잠긴 계정 없음")
		void success_noLockedAccounts() throws Exception {
			mockMvc.perform(get("/api/admin/auth/security/locked-accounts")
					.header("Authorization", "Bearer " + adminToken)
					.contentType(MediaType.APPLICATION_JSON))
					.andDo(print())
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value(200))
					.andExpect(jsonPath("$.data").isArray());
		}

		@Test
		@DisplayName("성공 - 잠긴 계정 있음")
		void success_hasLockedAccounts() throws Exception {
			// 테스트용 잠금 계정 생성
			redisTemplate.opsForValue().set(LOCK_KEY_PREFIX + "locked@test.com", "locked", 600, TimeUnit.SECONDS);

			try {
				mockMvc.perform(get("/api/admin/auth/security/locked-accounts")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON))
						.andDo(print())
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.code").value(200))
						.andExpect(jsonPath("$.data").isArray())
						.andExpect(jsonPath("$.data.length()").value(1))
						.andExpect(jsonPath("$.data[0].email").value("locked@test.com"));
			} finally {
				redisTemplate.delete(LOCK_KEY_PREFIX + "locked@test.com");
			}
		}

		@Test
		@DisplayName("실패 - 일반 회원 권한")
		void fail_withMemberRole() throws Exception {
			mockMvc.perform(get("/api/admin/auth/security/locked-accounts")
					.header("Authorization", "Bearer " + memberToken)
					.contentType(MediaType.APPLICATION_JSON))
					.andDo(print())
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	@DisplayName("계정 잠금 해제 API")
	class UnlockAccountTest {

		@Test
		@DisplayName("성공")
		void success() throws Exception {
			// 테스트용 잠금 계정 생성
			String email = "tounlock@test.com";
			redisTemplate.opsForValue().set(LOCK_KEY_PREFIX + email, "locked", 600, TimeUnit.SECONDS);

			try {
				mockMvc.perform(delete("/api/admin/auth/security/locked-accounts/{email}", email)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON))
						.andDo(print())
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.code").value(200));

				// 잠금이 해제되었는지 확인
				Boolean exists = redisTemplate.hasKey(LOCK_KEY_PREFIX + email);
				assert Boolean.FALSE.equals(exists);
			} finally {
				redisTemplate.delete(LOCK_KEY_PREFIX + email);
			}
		}

		@Test
		@DisplayName("실패 - 잠금 상태가 아닌 계정")
		void fail_notLocked() throws Exception {
			mockMvc.perform(delete("/api/admin/auth/security/locked-accounts/{email}", "notlocked@test.com")
					.header("Authorization", "Bearer " + adminToken)
					.contentType(MediaType.APPLICATION_JSON))
					.andDo(print())
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.code").value(404));
		}

		@Test
		@DisplayName("실패 - 일반 회원 권한")
		void fail_withMemberRole() throws Exception {
			mockMvc.perform(delete("/api/admin/auth/security/locked-accounts/{email}", "test@test.com")
					.header("Authorization", "Bearer " + memberToken)
					.contentType(MediaType.APPLICATION_JSON))
					.andDo(print())
					.andExpect(status().isForbidden());
		}
	}
}
