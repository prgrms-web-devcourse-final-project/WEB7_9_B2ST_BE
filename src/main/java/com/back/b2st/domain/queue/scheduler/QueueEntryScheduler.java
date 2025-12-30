package com.back.b2st.domain.queue.scheduler;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.back.b2st.domain.queue.entity.Queue;
import com.back.b2st.domain.queue.repository.QueueRepository;
import com.back.b2st.domain.queue.service.QueueSchedulerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * QueueEntryScheduler
 *
 * - 대기열을 자동으로 흘려보내는 스케줄러
 * - WAITING 상태의 사용자를 ENTERABLE 상태로 자동 이동
 * - 사람이 버튼을 누르지 않아도 정해진 시간마다 서버가 알아서 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
@Profile("!test") // 테스트 환경에서는 비활성화
public class QueueEntryScheduler {

	private final QueueRepository queueRepository;
	private final QueueSchedulerService queueSchedulerService;
	private final RedissonClient redissonClient;


	@Value("${spring.data.redis.mode:single}")
	private String redisMode;

	@Value("${queue.scheduler.batch-size:10}")
	private int batchSize;

	@Value("${queue.scheduler.fixed-delay:10000}")
	private long fixedDelay;

	/**
	 * 자동 입장 처리
	 */
	@Scheduled(fixedDelayString = "${queue.scheduler.fixed-delay:10000}")
	public void autoProcessQueueEntries() {
		//  락 조건: redisMode 기준
		// - redisMode == "single": 단일 Redis → 락 없이 실행
		// - redisMode == "cluster": Redis Cluster → 리더 락 사용
		if ("single".equals(redisMode)) {
			log.debug("단일 Redis 모드 - 리더 락 없이 실행");
			processAllQueues();
			return;
		}

		// Redis Cluster 모드: 리더 락 사용
		String leaderLockKey = "queue:scheduler:leader";
		RLock leaderLock = redissonClient.getLock(leaderLockKey);

		try {
			// 리더 락 획득 시도
			long waitTime = 3;
			long leaseTime = 300; // 락 유지 시간

			boolean acquired = leaderLock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);

			if (!acquired) {
				log.debug("스케줄러 리더 락 획득 실패 (다른 서버에서 실행 중)");
				return;
			}

			log.debug("스케줄러 리더 락 획득 성공 - 모든 대기열 처리 시작 (lease time: {}초)", leaseTime);

			// 리더로 선출된 서버만 모든 대기열 처리
			processAllQueues();

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("스케줄러 리더 락 획득 중 인터럽트 발생", e);
		} catch (Exception e) {
			log.error("자동 입장 스케줄러 실패", e);
		} finally {
			// 리더 락 해제
			if (leaderLock.isHeldByCurrentThread()) {
				leaderLock.unlock();
				log.debug("스케줄러 리더 락 해제");
			}
		}
	}

	/**
	 * 모든 대기열 처리 (내부 메서드)
	 *
	 * 리더 락을 획득한 서버만 이 메서드를 실행
	 */
	private void processAllQueues() {
		try {
			// 1. 모든 활성 대기열 조회
			List<Queue> activeQueues = queueRepository.findAll();

			if (activeQueues.isEmpty()) {
				log.debug("활성 대기열 없음");
				return;
			}

			// 2. 각 대기열별로 자동 입장 처리
			for (Queue queue : activeQueues) {
				try {
					// batchSize는 application.yml에서 설정 (몇명으로 하지)
					// queueId별 락은 제거하고 리더 락만 사용
					queueSchedulerService.processNextEntries(queue.getId(), batchSize);
				} catch (Exception e) {
					log.error("대기열 자동 입장 처리 실패 - queueId: {}", queue.getId(), e);
				}
			}

		} catch (Exception e) {
			log.error("대기열 처리 중 오류 발생", e);
		}
	}
}

