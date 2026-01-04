package com.back.b2st.domain.queue.scheduler;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 스케줄러 리더 락 실행기
 *
 * - redisMode=single: 락 없이 실행
 * - redisMode=cluster: 리더 락 획득한 인스턴스만 실행
 *
 * ✅ watchdog 활성화: tryLock(waitTime, unit) (leaseTime 미지정)
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
@Profile("!test")
public class SchedulerLeaderLockExecutor {

	private final RedissonClient redissonClient;

	@Value("${spring.data.redis.mode:single}")
	private String redisMode;

	@Value("${queue.scheduler.leader-lock.wait-seconds:3}")
	private long waitSeconds;

	public void runWithLeaderLock(String lockKey, Runnable task) {
		if ("single".equalsIgnoreCase(redisMode)) {
			task.run();
			return;
		}

		RLock lock = redissonClient.getLock(lockKey);

		try {
			boolean acquired = lock.tryLock(waitSeconds, TimeUnit.SECONDS); // ✅ watchdog ON
			if (!acquired) {
				log.debug("Leader lock not acquired - key={}", lockKey);
				return;
			}

			task.run();

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("Leader lock interrupted - key={}", lockKey, e);
		} catch (Exception e) {
			log.error("Leader lock task failed - key={}", lockKey, e);
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}
}
