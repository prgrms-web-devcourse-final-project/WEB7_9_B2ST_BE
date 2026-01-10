package com.back.b2st.domain.queue.scheduler;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.back.b2st.domain.queue.entity.QueueEntry;
import com.back.b2st.domain.queue.entity.QueueEntryStatus;
import com.back.b2st.domain.queue.repository.QueueEntryRepository;
import com.back.b2st.domain.queue.repository.QueueRedisRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DB ENTERABLE 정리 (SoT=Redis ENTERABLE ZSET 정책 유지)
 *
 * 1) expiresAt <= now 인 ENTERABLE -> EXPIRED (Redis와 무관)
 * 2) (옵션) expiresAt > now 인데 Redis ZSET에 없으면 stale로 보고 EXPIRED
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
@Profile("!test")
public class QueueEntryCleanupScheduler {

	private static final int BATCH_SIZE = 500;
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final QueueEntryRepository queueEntryRepository;
	private final QueueRedisRepository queueRedisRepository;
	private final SchedulerLeaderLockExecutor lockExecutor;
	private final TransactionTemplate transactionTemplate;

	@Value("${queue.cleanup.stale.enabled:false}")
	private boolean staleCleanupEnabled;

	@Scheduled(fixedDelayString = "${queue.cleanup.expired.fixedDelayMs:60000}")
	public void expireDbEnterablesByExpiresAt() {
		lockExecutor.runWithLeaderLock("queue:scheduler:leader:db-expire-by-expiresAt", this::doExpireByExpiresAtLoop);
	}

	private void doExpireByExpiresAtLoop() {
		try {
			LocalDateTime now = LocalDateTime.now(KST);
			int totalUpdated = 0;

			while (true) {
				Integer batchCount = transactionTemplate.execute(status -> {
					List<QueueEntry> targets = queueEntryRepository.findExpiredByStatus(
						QueueEntryStatus.ENTERABLE,
						now,
						PageRequest.of(0, BATCH_SIZE)
					);

					if (targets.isEmpty()) return 0;

					for (QueueEntry entry : targets) {
						entry.updateToExpired();
					}
					return targets.size(); // dirty checking
				});

				if (batchCount == null || batchCount == 0) break;
				totalUpdated += batchCount;
			}

			if (totalUpdated > 0) {
				log.info("DB ENTERABLE expired by expiresAt - updated: {}", totalUpdated);
			}
		} catch (Exception e) {
			log.error("Failed to expire DB ENTERABLE by expiresAt", e);
		}
	}

	@Scheduled(fixedDelayString = "${queue.cleanup.stale.fixedDelayMs:60000}")
	public void expireStaleDbEnterablesWithoutRedisZset() {
		if (!staleCleanupEnabled) return;

		lockExecutor.runWithLeaderLock("queue:scheduler:leader:db-expire-stale-without-redis", this::doExpireStaleOneShot);
	}

	/**
	 * stale 정리는 무한루프 위험이 있어 1회 처리(one-shot)로 안전하게 운영
	 */
	private void doExpireStaleOneShot() {
		try {
			LocalDateTime now = LocalDateTime.now(KST);

			Integer expiredCount = transactionTemplate.execute(status -> {
				List<QueueEntry> candidates = queueEntryRepository.findExpiredByStatus(
					QueueEntryStatus.ENTERABLE,
					now,
					PageRequest.of(0, BATCH_SIZE)
				);

				if (candidates.isEmpty()) return 0;

				int expiredInBatch = 0;

				for (QueueEntry entry : candidates) {
					Long queueId = entry.getQueueId();
					Long userId = entry.getUserId();

					boolean inEnterable;
					try {
						inEnterable = queueRedisRepository.isInEnterable(queueId, userId);
					} catch (Exception e) {
						log.warn("Redis check failed, skip stale cleanup - queueId: {}, userId: {}", queueId, userId, e);
						continue;
					}

					if (!inEnterable) {
						entry.updateToExpired();
						expiredInBatch++;
					}
				}

				return expiredInBatch;
			});

			if (expiredCount != null && expiredCount > 0) {
				log.info("DB stale ENTERABLE expired (no Redis ZSET entry) - updated: {}", expiredCount);
			}
		} catch (Exception e) {
			log.error("Failed to expire stale DB ENTERABLE without Redis ZSET", e);
		}
	}
}
