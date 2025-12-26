package com.back.b2st.domain.queue.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.queue.entity.QueueEntry;
import com.back.b2st.domain.queue.entity.QueueEntryStatus;
import com.back.b2st.domain.queue.repository.QueueEntryRepository;
import com.back.b2st.domain.queue.repository.QueueRedisRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * QueueExpireScheduler
 *
 * 📌 역할
 * - 입장 완료(ENTERABLE) 상태인데 결제 시간을 초과한 사용자를 자동으로 만료(EXPIRED) 처리
 * - 시간이 지난 입장권을 정리하여 다음 대기자가 들어올 수 있게 함
 *
 * 왜 필요한가?
 * - ENTERABLE 상태는 일정 시간(예: 15분) 동안만 유효
 * - 시간 내에 결제하지 않으면 자리를 반납해야 함
 * - 그렇지 않으면 뒤에 있는 대기자들이 영원히 못 들어옴
 *
 * 핵심 동작:
 * 1. DB에서 만료 시간이 지난 ENTERABLE 항목 조회
 * 2. Redis에서도 제거되었는지 확인
 * 3. EXPIRED 상태로 변경 및 Redis 정리
 *
 * ⚠️ 주의사항
 * - @Profile("!test")로 테스트 환경에서는 비활성화 (수동 제어)
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
@Profile("!test") // 테스트 환경에서는 비활성화
public class QueueExpireScheduler {

	private final QueueEntryRepository queueEntryRepository;
	private final QueueRedisRepository queueRedisRepository;

	/**
	 * 자동 만료 처리
	 *
	 * 실행 주기: 1분마다
	 * - 만료 시간이 지난 ENTERABLE 항목을 EXPIRED로 변경
	 * - Redis에서도 제거하여 정합성 유지
	 *
	 * @Scheduled(fixedDelay = 60000)
	 * - 이전 실행이 완료된 후 1분 뒤에 다시 실행
	 */
	@Scheduled(fixedDelay = 60000) // 1분마다
	@Transactional
	public void autoExpireEntries() {
		try {
			LocalDateTime now = LocalDateTime.now();

			// 1. 만료 시간이 지난 ENTERABLE 항목 조회
			List<QueueEntry> expiredCandidates = queueEntryRepository
				.findAllExpiredEntries(now);

			if (expiredCandidates.isEmpty()) {
				log.debug("만료 대상 없음");
				return;
			}

			log.info("만료 대상: {}명", expiredCandidates.size());

			int expiredCount = 0;

			// 2. 각 항목 만료 처리
			for (QueueEntry entry : expiredCandidates) {
				try {
					// 2-1. EXPIRED 상태로 변경
					if (entry.getStatus() == QueueEntryStatus.ENTERABLE) {
						entry.updateToExpired();
						expiredCount++;

						// 2-2. Redis에서도 제거
						try {
							queueRedisRepository.removeFromEnterable(
								entry.getQueueId(),
								entry.getUserId()
							);
						} catch (Exception e) {
							log.warn("Redis 제거 실패 (DB는 정상 처리) - queueId: {}, userId: {}",
								entry.getQueueId(), entry.getUserId(), e);
						}
					}
				} catch (Exception e) {
					log.error("항목 만료 처리 실패 - entryId: {}", entry.getId(), e);
				}
			}

			// 3. 변경사항 일괄 저장
			if (expiredCount > 0) {
				queueEntryRepository.saveAll(expiredCandidates);
				log.info("만료 처리 완료: {}명", expiredCount);
			}

		} catch (Exception e) {
			// 스케줄러는 절대 죽으면 안 됨
			log.error("자동 만료 스케줄러 실패", e);
		}
	}
}

