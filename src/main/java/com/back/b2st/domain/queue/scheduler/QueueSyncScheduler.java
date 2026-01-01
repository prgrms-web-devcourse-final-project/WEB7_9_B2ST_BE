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
	 */
	@Scheduled(fixedRate = 5000) // 5초마다
	@Transactional
	public void syncExpiredEntries() {
		try {
			LocalDateTime now = LocalDateTime.now();

			// DB에서 만료 예정 항목 조회
			List<QueueEntry> expiredCandidates = queueEntryRepository.findAllExpiredEntries(now);

			if (expiredCandidates.isEmpty()) {
				return;
			}

			int expiredCount = 0;

			// Redis에서 실제로 만료되었는지 확인 후 DB 업데이트
			for (QueueEntry entry : expiredCandidates) {
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
	 * Redis SET 정리
	 *
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
				Long removed = queueRedisRepository.cleanupExpiredFromIndex(queueId);
				if (removed != null && removed > 0) {
					log.debug("ZSET 정리: 만료된 토큰 {}건 제거 - queueId: {}", removed, queueId);
				}
			}

		} catch (Exception e) {
			log.error("Failed to cleanup expired users from SET", e);
		}
	}

	/**
	 * DB-Redis 정합성 보정 스케줄러
	 */
	@Scheduled(fixedRate = 180000) // 3분마다 (최종일관성 지연 최대값)
	@Transactional
	public void syncRedisWithDb() {
		try {
			LocalDateTime now = LocalDateTime.now();

			// ==================== 1. DB 기준 보정 (조건 강화) ====================
			// DB에서 ENTERABLE 상태인 모든 엔트리 조회
			List<QueueEntry> enterableEntries = queueEntryRepository.findAllByStatus(
				com.back.b2st.domain.queue.entity.QueueEntryStatus.ENTERABLE
			);

			int dbExpiredCount = 0;  // DB에서 만료 처리된 항목 (expiresAt 기준)
			int redisMissingExpiredCount = 0;  // Redis에 없고 만료된 항목 (grace period 후)

			for (QueueEntry entry : enterableEntries) {
				Long queueId = entry.getQueueId();
				Long userId = entry.getUserId();

				try {
					// 1: 만료 시간 확인 필수
					boolean isExpired = entry.getExpiresAt() != null
						&& entry.getExpiresAt().isBefore(now);

					if (isExpired) {
						// 만료 시간이 지났으면 EXPIRED로 변경
						entry.updateToExpired();
						dbExpiredCount++;

						// Redis에서도 제거 (있을 경우)
						try {
							if (queueRedisRepository.isInEnterable(queueId, userId)) {
								queueRedisRepository.removeFromEnterable(queueId, userId);
							}
						} catch (Exception e) {
							log.warn("만료 항목 Redis 제거 실패 - queueId: {}, userId: {}", queueId, userId, e);
						}
						continue;
					}

					// Redis에 ENTERABLE로 존재하는지 확인
					boolean inRedis = queueRedisRepository.isInEnterable(queueId, userId);

					if (!inRedis) {
						// Redis에 없지만 expiresAt이 지나지 않았으면 Grace Period 적용
						// → 일시적 Redis 장애나 TTL 만료 직후일 수 있음
						// → expiresAt이 지나지 않았으면 다음 주기에서 재확인
						// 지금은 expiresAt이 지나지 않았으면 대기
						// expiresAt이 지났으면 만료 처리
						if (entry.getExpiresAt() != null && entry.getExpiresAt().isBefore(now)) {
							entry.updateToExpired();
							redisMissingExpiredCount++;
							log.debug("정합성 보정: Redis에 없고 만료되어 DB 만료 처리 - queueId: {}, userId: {}", queueId, userId);
						} else {
							// Grace Period: expiresAt이 지나지 않았으면 다음 주기에서 재확인
							log.debug("Grace Period: Redis에 없지만 만료 전 - queueId: {}, userId: {}, expiresAt: {}",
								queueId, userId, entry.getExpiresAt());
						}
					}
				} catch (Exception e) {
					log.warn("DB 기준 정합성 확인 실패 - queueId: {}, userId: {}", queueId, userId, e);
				}
			}

			// 1-2. DB 업데이트 (만료 처리된 항목)
			if (dbExpiredCount > 0 || redisMissingExpiredCount > 0) {
				queueEntryRepository.saveAll(enterableEntries);
			}

			// ==================== 2. Redis 기준 보정 ====================
			// Redis에만 있는 ENTERABLE 제거 (DB에 없는 경우)
			int redisCleanedCount = 0;

			// 2-1. 모든 활성 대기열의 Redis ENTERABLE 확인
			List<Long> activeQueueIds = enterableEntries.stream()
				.map(QueueEntry::getQueueId)
				.distinct()
				.toList();

			// DB에 없는 대기열도 확인하기 위해 추가 조회
			if (activeQueueIds.isEmpty()) {
				// DB에 ENTERABLE이 없으면 모든 대기열 확인
				activeQueueIds = queueEntryRepository.findAll().stream()
					.map(QueueEntry::getQueueId)
					.distinct()
					.toList();
			}

			for (Long queueId : activeQueueIds) {
				try {
					var redisEnterableUsers = queueRedisRepository.getAllEnterableUsers(queueId);
					if (redisEnterableUsers == null || redisEnterableUsers.isEmpty()) {
						continue;
					}

					// Redis에 있는 각 사용자가 DB에도 있는지 확인
					for (Object userIdObj : redisEnterableUsers) {
						Long userId = Long.parseLong(userIdObj.toString());

						// DB에 해당 사용자의 ENTERABLE 엔트리가 있는지 확인
						boolean existsInDb = enterableEntries.stream()
							.anyMatch(e -> e.getQueueId().equals(queueId)
								&& e.getUserId().equals(userId)
								&& e.getStatus() == com.back.b2st.domain.queue.entity.QueueEntryStatus.ENTERABLE);

						if (!existsInDb) {
							queueRedisRepository.removeFromEnterable(queueId, userId);
							redisCleanedCount++;
							log.debug("정합성 보정: DB에 없어서 Redis에서 제거 - queueId: {}, userId: {}", queueId, userId);
						}
					}
				} catch (Exception e) {
					log.warn("Redis 기준 정합성 보정 실패 - queueId: {}", queueId, e);
				}
			}

			// ==================== 3. 결과 로깅 ====================
			if (dbExpiredCount > 0 || redisMissingExpiredCount > 0 || redisCleanedCount > 0) {
				log.info("DB-Redis 정합성 보정 완료 - DB 만료(expiresAt): {}건, DB 만료(Redis 없음+만료): {}건, Redis 정리(DB 없음): {}건",
					dbExpiredCount, redisMissingExpiredCount, redisCleanedCount);
			}

		} catch (Exception e) {
			log.error("DB-Redis 정합성 보정 실패", e);
		}
	}
}

