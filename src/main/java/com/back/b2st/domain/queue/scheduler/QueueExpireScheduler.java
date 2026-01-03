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
 * - 입장 완료(ENTERABLE) 상태인데 결제 시간을 초과한 사용자를 자동으로 만료(EXPIRED) 처리
 * - 시간이 지난 입장권을 정리하여 다음 대기자가 들어올 수 있게 함
 *
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
	 * @Scheduled(fixedDelay = 60000)
	 * - 이전 실행이 완료된 후 1분 뒤에 다시 실행
	 */
	@Scheduled(fixedDelay = 60000) // 1분마다
	@Transactional
	public void autoExpireEntries() {
		try {
			LocalDateTime now = LocalDateTime.now();

			// 1. 만료 시간이 지난 ENTERABLE 항목 조회 (DB 기준)
			List<QueueEntry> expiredCandidates = queueEntryRepository
				.findAllExpiredEntries(now);

			if (expiredCandidates.isEmpty()) {
				log.debug("만료 대상 없음");
				return;
			}

			log.info("만료 대상: {}명", expiredCandidates.size());

			int expiredCount = 0;
			int redisRemovedCount = 0;

			// 2. 각 항목 만료 처리
			for (QueueEntry entry : expiredCandidates) {
				try {
					// 2-1. EXPIRED 상태로 변경
					if (entry.getStatus() == QueueEntryStatus.ENTERABLE) {
						entry.updateToExpired();
						expiredCount++;

						// 2-2. Redis에서도 제거
						try {
							if (queueRedisRepository.isInEnterable(entry.getQueueId(), entry.getUserId())) {
								queueRedisRepository.removeFromEnterable(
									entry.getQueueId(),
									entry.getUserId()
								);
								redisRemovedCount++;
							}
						} catch (Exception e) {
							log.warn("Redis 제거 실패 (DB는 정상 처리) - queueId: {}, userId: {}",
								entry.getQueueId(), entry.getUserId(), e);
						}
					}
				} catch (Exception e) {
					log.error("항목 만료 처리 실패 - entryId: {}", entry.getId(), e);
				}
			}

			if (expiredCount > 0) {
				queueEntryRepository.saveAll(expiredCandidates);
				log.info("만료 처리 완료: {}명 (Redis 제거: {}명)", expiredCount, redisRemovedCount);
			}

		} catch (Exception e) {
			log.error("자동 만료 스케줄러 실패", e);
		}
	}
}

