package com.back.b2st.domain.queue.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.queue.entity.QueueEntry;
import com.back.b2st.domain.queue.repository.QueueEntryRepository;
import com.back.b2st.domain.queue.repository.QueueRedisRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Queue 동기화 스케줄러
 *
 * Redis와 DB 간의 데이터 정합성을 유지하기 위한 배치 작업
 *
 * 개발 초기 단계 - 대기열 기능 활성화 시에만 로드
 * application.yml에서 `queue.enabled: true` 및 `queue.sync.enabled: true` 설정 필요
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
public class QueueSyncScheduler {

	private final QueueEntryRepository queueEntryRepository;
	private final QueueRedisRepository queueRedisRepository;

	/**
	 * Redis TTL 만료된 항목을 DB에 동기화
	 *
	 * 실행 주기: 5초마다
	 * - Redis에서 TTL로 자동 만료된 입장권을 DB에도 EXPIRED로 업데이트
	 * - Redis와 DB 간의 상태 불일치 방지
	 *
	 * 대규모 트래픽 환경에서 중요:
	 * - 초당 수천 건의 만료 처리 가능
	 * - 사용자 경험 보장 (재입장 시 정확한 상태 확인)
	 */
	@Scheduled(fixedRate = 5000) // 5초마다
	@Transactional
	public void syncExpiredEntries() {
		try {
			LocalDateTime now = LocalDateTime.now();

			// DB에서 만료 예정 항목 조회 (인덱스 활용: status, expires_at)
			List<QueueEntry> expiredCandidates = queueEntryRepository.findAllExpiredEntries(now);

			if (expiredCandidates.isEmpty()) {
				return;
			}

			int expiredCount = 0;

			// Redis에서 실제로 만료되었는지 확인 후 DB 업데이트
			for (QueueEntry entry : expiredCandidates) {
				// Redis에 입장권이 없으면 만료 처리
				if (!queueRedisRepository.isInEnterable(entry.getQueueId(), entry.getUserId())) {
					entry.updateToExpired();
					expiredCount++;
				}
			}

			if (expiredCount > 0) {
				queueEntryRepository.saveAll(expiredCandidates);
				log.info("Synced {} expired entries to DB", expiredCount);
			}

		} catch (Exception e) {
			log.error("Failed to sync expired entries", e);
		}
	}

	/**
	 * Redis SET 정리 작업
	 * 실행 주기: 1분마다
	 * - ENTERABLE_SET에서 실제로는 만료된 userId 제거
	 * - Redis 메모리 최적화
	 */
	@Scheduled(fixedRate = 60000) // 1분마다
	public void cleanupExpiredFromSet() {
		try {
			// 모든 활성 대기열 조회
			List<Long> activeQueueIds = queueEntryRepository.findAll().stream()
				.map(entry -> entry.getQueueId())
				.distinct()
				.toList();

			for (Long queueId : activeQueueIds) {
				var enterableUsers = queueRedisRepository.getAllEnterableUsers(queueId);

				if (enterableUsers == null || enterableUsers.isEmpty()) {
					continue;
				}

				int removedCount = 0;
				for (Object userIdObj : enterableUsers) {
					Long userId = Long.parseLong(userIdObj.toString());

					// 개별 User Key가 없으면 SET에서도 제거
					if (!queueRedisRepository.isInEnterable(queueId, userId)) {
						queueRedisRepository.removeFromEnterable(queueId, userId);
						removedCount++;
					}
				}

				if (removedCount > 0) {
					log.debug("Cleaned up {} expired users from SET - queueId: {}", removedCount, queueId);
				}
			}

		} catch (Exception e) {
			log.error("Failed to cleanup expired users from SET", e);
		}
	}
}

