package com.back.b2st.domain.email.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.email.dto.request.CheckDuplicateReq;
import com.back.b2st.domain.email.dto.request.VerifyCodeReq;
import com.back.b2st.domain.email.dto.response.CheckDuplicateRes;
import com.back.b2st.domain.email.entity.EmailVerification;
import com.back.b2st.domain.email.error.EmailErrorCode;
import com.back.b2st.domain.email.repository.EmailVerificationRepository;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
	@InjectMocks
	private EmailService emailService;
	@Mock
	private EmailVerificationRepository emailVerificationRepository;
	@Mock
	private MemberRepository memberRepository;
	@Mock
	private EmailRateLimiter rateLimiter;

	@Test
	@DisplayName("중복 확인 - 사용 가능한 이메일")
	void checkDuplicate_available() {
		// given
		String email = "new@test.com";
		given(memberRepository.existsByEmail(email)).willReturn(false);
		// when
		CheckDuplicateRes result = emailService.checkDuplicate(new CheckDuplicateReq(email));
		// then
		assertThat(result.available()).isTrue();
	}

	@Test
	@DisplayName("중복 확인 - 이미 가입된 이메일")
	void checkDuplicate_alreadyExists() {
		// given
		String email = "exists@test.com";
		given(memberRepository.existsByEmail(email)).willReturn(true);
		// when
		CheckDuplicateRes result = emailService.checkDuplicate(new CheckDuplicateReq(email));
		// then
		assertThat(result.available()).isFalse();
	}
	
	@Test
	@DisplayName("인증 성공 - 코드 일치")
	void verifyCode_success() {
		// given
		String email = "test@test.com";
		String code = "123456";
		EmailVerification verification = EmailVerification.builder()
			.email(email)
			.code(code)
			.attemptCount(0)
			.build();
		Member member = Member.builder()
			.email(email)
			.name("테스터")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(false)
			.isIdentityVerified(false)
			.build();
		given(emailVerificationRepository.findById(email)).willReturn(Optional.of(verification));
		given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
		// when
		emailService.verifyCode(new VerifyCodeReq(email, code));
		// then
		assertThat(member.isEmailVerified()).isTrue();
		verify(emailVerificationRepository).deleteById(email);
	}

	@Test
	@DisplayName("인증 실패 - 코드 불일치")
	void verifyCode_wrongCode() {
		// given
		String email = "test@test.com";
		EmailVerification verification = EmailVerification.builder()
			.email(email)
			.code("123456")
			.attemptCount(0)
			.build();
		given(emailVerificationRepository.findById(email)).willReturn(Optional.of(verification));
		// when & then
		assertThatThrownBy(() -> emailService.verifyCode(new VerifyCodeReq(email, "000000")))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(EmailErrorCode.VERIFICATION_CODE_MISMATCH);
		verify(emailVerificationRepository).save(any(EmailVerification.class));
	}

	@Test
	@DisplayName("인증 실패 - 시도 횟수 초과")
	void verifyCode_maxAttempt() {
		// given
		String email = "test@test.com";
		EmailVerification verification = EmailVerification.builder()
			.email(email)
			.code("123456")
			.attemptCount(5)  // 이미 5회 시도
			.build();
		given(emailVerificationRepository.findById(email)).willReturn(Optional.of(verification));
		// when & then
		assertThatThrownBy(() -> emailService.verifyCode(new VerifyCodeReq(email, "123456")))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(EmailErrorCode.VERIFICATION_MAX_ATTEMPT);
		verify(emailVerificationRepository).deleteById(email);
	}

	@Test
	@DisplayName("인증 실패 - 인증 정보 없음")
	void verifyCode_notFound() {
		// given
		String email = "test@test.com";
		given(emailVerificationRepository.findById(email)).willReturn(Optional.empty());
		// when & then
		assertThatThrownBy(() -> emailService.verifyCode(new VerifyCodeReq(email, "123456")))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(EmailErrorCode.VERIFICATION_NOT_FOUND);
	}
}
