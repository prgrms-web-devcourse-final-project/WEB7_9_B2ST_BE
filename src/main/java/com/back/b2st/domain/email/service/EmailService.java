package com.back.b2st.domain.email.service;

import static com.back.b2st.global.util.MaskingUtil.*;

import java.security.SecureRandom;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.email.dto.request.CheckDuplicateReq;
import com.back.b2st.domain.email.dto.request.SenderVerificationReq;
import com.back.b2st.domain.email.dto.request.VerifyCodeReq;
import com.back.b2st.domain.email.dto.response.CheckDuplicateRes;
import com.back.b2st.domain.email.entity.EmailVerification;
import com.back.b2st.domain.email.error.EmailErrorCode;
import com.back.b2st.domain.email.metrics.EmailMetrics;
import com.back.b2st.domain.email.repository.EmailVerificationRepository;
import com.back.b2st.domain.lottery.result.dto.LotteryResultEmailInfo;
import com.back.b2st.domain.lottery.result.repository.LotteryResultRepository;
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

	// 코드 난수
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private final EmailVerificationRepository emailVerificationRepository;
	private final LotteryResultRepository lotteryResultRepository;
	private final EmailSender emailSender;
	private final MemberRepository memberRepository;
	private final EmailRateLimiter rateLimiter;
	private final EmailMetrics emailMetrics;

	/**
	 * 이메일 중복 확인 - existsBy 조회 최적화 + boolean 반전
	 *
	 * @param request 이메일 중복 확인 요청
	 * @return 사용 가능 여부
	 */
	@Transactional(readOnly = true)
	public CheckDuplicateRes checkDuplicate(CheckDuplicateReq request) {
		boolean exists = memberRepository.existsByEmail(request.email());
		return new CheckDuplicateRes(!exists);
	}

	/**
	 * 인증 코드 발송 - 이미 인증 회원 체크 + Rate Limiting + SecureRandom + Redis 저장 + 비동기 발송
	 *
	 * @param request 인증 코드 발송 요청
	 */
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
			emailMetrics.recordEmailSent(true);
		} catch (Exception e) {
			emailMetrics.recordEmailSent(false);
			log.error("이메일 발송 요청 실패: {}", e.getMessage());
			// 저장은 완료되었으니 예외 throw하지 않음
		}
	}

	/**
	 * 인증 코드 검증 - 시도 횟수 제한(5회) + 코드 검증 + Redis 삭제 + 회원 상태 갱신
	 *
	 * @param request 인증 코드 검증 요청
	 */
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
			emailMetrics.recordVerification(false);
			throw new BusinessException(EmailErrorCode.VERIFICATION_MAX_ATTEMPT);
		}

		// 코드 일치 확인
		if (!verification.getCode().equals(inputCode)) {
			// 시도 횟수 증가 후 저장
			EmailVerification updated = verification.incrementAttempt();
			emailVerificationRepository.save(updated);

			emailMetrics.recordVerification(false);
			log.warn("인증 코드 불일치: email={}, attempt={}", maskEmail(email), updated.getAttemptCount());
			throw new BusinessException(EmailErrorCode.VERIFICATION_CODE_MISMATCH);
		}

		// 성공하면 redis서 삭제
		emailVerificationRepository.deleteById(email);

		// 회원이 이미 있으면 isEmailVerified = true로 업데이트
		// 회원이 없으면 (회원가입 전 인증) 스킵 → 회원가입 시 isEmailVerified=true로 생성
		memberRepository.findByEmail(email)
			.ifPresent(Member::verifyEmail);

		emailMetrics.recordVerification(true);
		log.info("이메일 인증 성공: email={}", maskEmail(email));
	}

	/**
	 * 특정 회차의 당첨자에게 이메일 발송
	 */
	@Transactional(readOnly = true)
	public void sendWinnerNotifications(Long scheduleId) {
		log.info("당첨자 이메일 발송 시작 - scheduleId: {}", scheduleId);

		List<LotteryResultEmailInfo> winners = lotteryResultRepository
			.findSendEmailInfoByScheduleId(scheduleId);

		if (winners.isEmpty()) {
			log.info("당첨자 없음 - scheduleId: {}", scheduleId);
			return;
		}

		log.debug("당첨자 수: {}", winners.size());

		int successCount = 0;
		int failCount = 0;

		for (LotteryResultEmailInfo winner : winners) {
			try {
				sendWinnerEmail(winner);
				successCount++;
			} catch (Exception e) {
				failCount++;
				log.error("이메일 발송 실패 - resultId: {}, memberId: {}, error: {}",
					winner.id(), winner.memberId(), e.getMessage());
			}
		}

		log.info("당첨자 이메일 발송 완료 - 성공: {}, 실패: {}", successCount, failCount);
	}

	/**
	 * 개별 당첨자에게 이메일 발송
	 */
	private void sendWinnerEmail(LotteryResultEmailInfo winner) {
		Member member = memberRepository.findById(winner.memberId())
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

		emailSender.sendLotteryWinnerEmail(
			member.getEmail(),
			winner.memberName(),
			winner.seatGrade(),
			winner.quantity(),
			winner.paymentDeadline()
		);

		log.info("당첨 안내 이메일 발송 완료 - email: {}, resultId: {}",
			maskEmail(member.getEmail()), winner.id());
	}

	/**
	 * 결제 기한 초과로 당첨 취소된 사용자에게 이메일 발송
	 */
	@Transactional(readOnly = true)
	public void sendCancelUnpaidNotifications(List<Long> memberIds) {
		log.info("당첨 취소 이메일 발송 시작");

		if (memberIds.isEmpty()) {
			log.info("당첨 취소 대상 없음");
			return;
		}

		int successCount = 0;
		int failCount = 0;

		for (Long memberId : memberIds) {
			try {
				sendCancelUnpaidEmail(memberId);
				successCount++;
			} catch (Exception e) {
				failCount++;
				log.error("당첨 취소 이메일 발송 실패 - memberId: {}", memberId, e);
			}
		}

		log.info("당첨 취소 이메일 발송 완료 - 성공: {}, 실패: {}", successCount, failCount);
	}

	/**
	 * 취소 안내 메일 발송
	 */
	private void sendCancelUnpaidEmail(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

		emailSender.sendCancelUnpaidEmail(
			member.getEmail(),
			member.getName()
		);

		log.info("당첨 안내 이메일 발송 완료 - email: {}, resultId: {}",
			maskEmail(member.getEmail()), member.getId());
	}

	// 밑으로 헬퍼 메서드
	// 보안 코드 생성
	private String generateSecureCode() {
		int code = 100000 + SECURE_RANDOM.nextInt(900000);
		return String.valueOf(code);
	}
}
