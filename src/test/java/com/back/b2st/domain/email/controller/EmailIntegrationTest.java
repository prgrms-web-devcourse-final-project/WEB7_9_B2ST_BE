package com.back.b2st.domain.email.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.email.entity.EmailVerification;
import com.back.b2st.domain.email.repository.EmailVerificationRepository;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.global.test.AbstractContainerBaseTest;

import jakarta.mail.internet.MimeMessage;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmailIntegrationTest extends AbstractContainerBaseTest {

	private static final String TEST_EMAIL = "integration-test@example.com";
	// 실제 SMTP 연결 없이 테스트
	@MockitoBean
	private JavaMailSender javaMailSender;
	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private EmailVerificationRepository emailVerificationRepository;

	@BeforeEach
	void setUp() {
		emailVerificationRepository.deleteAll();
		memberRepository.deleteAll();

		MimeMessage mockMimeMessage = mock(MimeMessage.class);
		when(javaMailSender.createMimeMessage()).thenReturn(mockMimeMessage);
	}

	@Test
	@DisplayName("통합: 이메일 중복 확인 - 사용 가능")
	void checkDuplicate_available() throws Exception {
		// when & then
		mockMvc.perform(post("/api/email/check-duplicate")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"available@test.com\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.available").value(true));
	}

	@Test
	@DisplayName("통합: 이메일 중복 확인 - 이미 가입됨")
	void checkDuplicate_alreadyExists() throws Exception {
		// given
		createTestMember(TEST_EMAIL);
		// when & then
		mockMvc.perform(post("/api/email/check-duplicate")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"" + TEST_EMAIL + "\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.available").value(false));
	}

	@Test
	@DisplayName("통합: 인증 코드 발송 성공")
	void sendVerificationCode_success() throws Exception {
		// when
		mockMvc.perform(post("/api/email/verification")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"" + TEST_EMAIL + "\"}"))
			.andExpect(status().isOk());
		// then
		assertThat(emailVerificationRepository.findById(TEST_EMAIL)).isPresent();
	}

	@Test
	@DisplayName("통합: 잘못된 이메일 형식")
	void sendVerificationCode_invalidEmail() throws Exception {
		mockMvc.perform(post("/api/email/verification")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"invalid-email\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("통합: 인증 코드 검증 성공")
	void verifyCode_success() throws Exception {
		// given
		Member member = createTestMember(TEST_EMAIL);
		String code = "123456";
		emailVerificationRepository.save(EmailVerification.builder()
			.email(TEST_EMAIL)
			.code(code)
			.attemptCount(0)
			.build());
		// when
		mockMvc.perform(post("/api/email/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"" + TEST_EMAIL + "\", \"code\": \"" + code + "\"}"))
			.andExpect(status().isOk());
		// then
		Member updatedMember = memberRepository.findByEmail(TEST_EMAIL).orElseThrow();
		assertThat(updatedMember.isEmailVerified()).isTrue();
		assertThat(emailVerificationRepository.findById(TEST_EMAIL)).isEmpty();
	}

	@Test
	@DisplayName("통합: 인증 코드 불일치")
	void verifyCode_wrongCode() throws Exception {
		// given
		createTestMember(TEST_EMAIL);
		emailVerificationRepository.save(EmailVerification.builder()
			.email(TEST_EMAIL)
			.code("123456")
			.attemptCount(0)
			.build());
		// when & then
		mockMvc.perform(post("/api/email/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"" + TEST_EMAIL + "\", \"code\": \"000000\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(400));
		EmailVerification verification = emailVerificationRepository.findById(TEST_EMAIL).orElseThrow();
		assertThat(verification.getAttemptCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("통합: 시도 횟수 초과")
	void verifyCode_maxAttempt() throws Exception {
		// given
		createTestMember(TEST_EMAIL);
		emailVerificationRepository.save(EmailVerification.builder()
			.email(TEST_EMAIL)
			.code("123456")
			.attemptCount(5)
			.build());
		// when & then
		mockMvc.perform(post("/api/email/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"" + TEST_EMAIL + "\", \"code\": \"123456\"}"))
			.andExpect(status().isTooManyRequests());
		assertThat(emailVerificationRepository.findById(TEST_EMAIL)).isEmpty();
	}

	@Test
	@DisplayName("E2E: 전체 인증 플로우")
	void fullVerificationFlow() throws Exception {
		// 이메일 중복 확인
		mockMvc.perform(post("/api/email/check-duplicate")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"" + TEST_EMAIL + "\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.available").value(true));
		// 인증 코드 발송
		mockMvc.perform(post("/api/email/verification")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"" + TEST_EMAIL + "\"}"))
			.andExpect(status().isOk());
		// Redis에서 코드 확인
		EmailVerification verification = emailVerificationRepository.findById(TEST_EMAIL)
			.orElseThrow();
		String code = verification.getCode();
		// 회원 생성
		createTestMember(TEST_EMAIL);
		// 인증 코드 검증
		mockMvc.perform(post("/api/email/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"" + TEST_EMAIL + "\", \"code\": \"" + code + "\"}"))
			.andExpect(status().isOk());
		// 최종 확인
		Member member = memberRepository.findByEmail(TEST_EMAIL).orElseThrow();
		assertThat(member.isEmailVerified()).isTrue();
	}

	// 밑으로 헬퍼
	private Member createTestMember(String email) {
		Member member = Member.builder()
			.email(email)
			.password("encodedPassword")
			.name("테스터")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(false)
			.isIdentityVerified(false)
			.build();
		return memberRepository.save(member);
	}
}
