package com.back.b2st.domain.email.service;

import static com.back.b2st.global.util.MaskingUtil.*;

import java.security.SecureRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.email.dto.request.CheckDuplicateReq;
import com.back.b2st.domain.email.dto.request.SenderVerificationReq;
import com.back.b2st.domain.email.dto.request.VerifyCodeReq;
import com.back.b2st.domain.email.dto.response.CheckDuplicateRes;
import com.back.b2st.domain.email.entity.EmailVerification;
import com.back.b2st.domain.email.error.EmailErrorCode;
import com.back.b2st.domain.email.repository.EmailVerificationRepository;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.error.MemberErrorCode;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

	private final EmailVerificationRepository emailVerificationRepository;
	private final EmailSender emailSender;
	private final MemberRepository memberRepository;
	private final EmailRateLimiter rateLimiter;

	// 코드 난수
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	// existsBy 조회 최적화 + boolean 반전
	@Transactional(readOnly = true)
	public CheckDuplicateRes checkDuplicate(CheckDuplicateReq request) {
		boolean exists = memberRepository.existsByEmail(request.email());
		return new CheckDuplicateRes(!exists);
	}

	// 이미 인증 회원 체크 + Rate Limiting + SecureRandom + Redis 저장 + 비동기 발송
	public void sendVerificationCode(SenderVerificationReq request) {
		String email = request.email();

		// 이미 인증된 회원인지 확인
		memberRepository.findByEmail(email).ifPresent(member -> {
			if (member.isEmailVerified()) {
				throw new BusinessException(EmailErrorCode.ALREADY_VERIFIED);
			}
		});

		rateLimiter.checkRateLimit(email);
		String code = generateSecureCode();

		// redis 저장. 기존 있으면 덮어쓰기
		EmailVerification emailVerification = EmailVerification.builder()
				.email(email)
				.code(code)
				.attemptCount(0)
				.build();

		emailVerificationRepository.save(emailVerification);

		log.info("인증 코드 저장 완료: email={}", maskEmail(email));

		// 비동기 발송
		try {
			emailSender.sendEmailAsync(email, code);
		} catch (Exception e) {
			log.error("이메일 발송 요청 실패: {}", e.getMessage());
			// 저장은 완료되었으니 예외 throw하지 않음
		}
	}

	// 시도 횟수 제한(5회) + 코드 검증 + Redis 삭제 + 회원 상태 갱신
	@Transactional
	public void verifyCode(VerifyCodeReq request) {
		String email = request.email();
		String inputCode = request.code();

		// redis 조회
		EmailVerification verification = emailVerificationRepository.findById(email)
				.orElseThrow(() -> new BusinessException(EmailErrorCode.VERIFICATION_NOT_FOUND));

		// 시도 횟수 확인
		if (verification.isMaxAttemptExceeded()) {
			emailVerificationRepository.deleteById(email);
			throw new BusinessException(EmailErrorCode.VERIFICATION_MAX_ATTEMPT);
		}

		// 코드 일치 확인
		if (!verification.getCode().equals(inputCode)) {
			// 시도 횟수 증가 후 저장
			EmailVerification updated = verification.incrementAttempt();
			emailVerificationRepository.save(updated);

			log.warn("인증 코드 불일치: email={}, attempt={}", maskEmail(email), updated.getAttemptCount());
			throw new BusinessException(EmailErrorCode.VERIFICATION_CODE_MISMATCH);
		}

		// 성공하면 redis서 삭제
		emailVerificationRepository.deleteById(email);

		// 회원이 이미 있으면 isEmailVerified = true로 업데이트
		// 회원이 없으면 (회원가입 전 인증) 스킵 → 회원가입 시 isEmailVerified=true로 생성
		memberRepository.findByEmail(email)
				.ifPresent(Member::verifyEmail);

		log.info("이메일 인증 성공: email={}", maskEmail(email));
	}

	// 밑으로 헬퍼 메서드
	// 보안 코드 생성
	private String generateSecureCode() {
		int code = 100000 + SECURE_RANDOM.nextInt(900000);
		return String.valueOf(code);
	}
}
