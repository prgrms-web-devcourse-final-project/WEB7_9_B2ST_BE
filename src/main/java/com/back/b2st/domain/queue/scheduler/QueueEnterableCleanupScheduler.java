package com.back.b2st.domain.queue.scheduler;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.back.b2st.domain.queue.entity.Queue;
import com.back.b2st.domain.queue.repository.QueueRedisRepository;
import com.back.b2st.domain.queue.repository.QueueRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ENTERABLE ZSET 만료(score < nowSeconds) 정리
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
@Profile("!test")
public class QueueEnterableCleanupScheduler {

	private final QueueRepository queueRepository;
	private final QueueRedisRepository queueRedisRepository;
	private final SchedulerLeaderLockExecutor lockExecutor;

	@Scheduled(fixedDelayString = "${queue.cleanup.enterable.fixedDelayMs:5000}")
	public void cleanupExpiredEnterable() {
		lockExecutor.runWithLeaderLock("queue:scheduler:leader:redis-enterable-cleanup", () -> {
			List<Queue> queues = queueRepository.findAll();
			if (queues.isEmpty()) return;

			long totalRemoved = 0;

			for (Queue queue : queues) {
				try {
					Long removed = queueRedisRepository.cleanupExpiredEnterable(queue.getId());
					if (removed != null && removed > 0) totalRemoved += removed;
				} catch (Exception e) {
					log.warn("ENTERABLE ZSET 정리 실패 - queueId={}", queue.getId(), e);
				}
			}

			if (totalRemoved > 0) {
				log.info("ENTERABLE ZSET 만료 정리 완료 - 총 제거: {}건", totalRemoved);
			}
		});
	}
}
