package com.back.b2st.domain.member.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberCleanupScheduler {

	// 탈퇴 유예 기간 30일
	private static final int WITHDRAWAL_GRACE_PERIOD_DAYS = 30;
	private final MemberRepository memberRepository;

	// 매일 새벽 3시
	@Scheduled(cron = "0 0 3 * * *")
	@Transactional
	public void processExpiredWithdrawals() {
		log.info("[MemberCleanup] 탈퇴 회원 익명화 처리 시작");

		LocalDateTime threshold = LocalDateTime.now().minusDays(WITHDRAWAL_GRACE_PERIOD_DAYS);

		List<Member> expiredMembers = memberRepository.findAllByDeletedAtBefore(threshold);
		if (expiredMembers.isEmpty()) {
			log.info("[MemberCleanup] 익명화 대상 회원 없음");
			return;
		}

		int processedCount = 0;
		for (Member member : expiredMembers) {
			try {
				if (member.getEmail().startsWith("withdrawn_")) {
					continue;
				}

				member.anonymize();
				processedCount++;

				log.info("[MemberCleanup] 회원 익명화 완료: MemberID={}", member.getId());

			} catch (Exception e) {
				log.error("[MemberCleanup] 회원 익명화 실패: MemberID={}, Error={}", member.getId(), e.getMessage(), e);
			}
		}
		log.info("[MemberCleanup] 탈퇴 회원 익명화 처리 완료: 처리 건수={}", processedCount);
	}
}
