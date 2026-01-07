package com.back.b2st.domain.queue.scheduler;

import java.util.List;

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
 * WAITING -> ENTERABLE 자동 처리 스케줄러
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
@Profile("!test")
public class QueueEntryScheduler {

	private final QueueRepository queueRepository;
	private final QueueSchedulerService queueSchedulerService;
	private final SchedulerLeaderLockExecutor lockExecutor;

	@Value("${queue.scheduler.batch-size:10}")
	private int batchSize;

	@Scheduled(fixedDelayString = "${queue.scheduler.fixed-delay:10000}")
	public void autoProcessQueueEntries() {
		lockExecutor.runWithLeaderLock("queue:scheduler:leader:entry", this::processAllQueues);
	}

	private void processAllQueues() {
		List<Queue> activeQueues = queueRepository.findAll();
		if (activeQueues.isEmpty()) return;

		for (Queue queue : activeQueues) {
			try {
				queueSchedulerService.processNextEntries(queue.getId(), batchSize);
			} catch (Exception e) {
				log.error("대기열 자동 입장 처리 실패 - queueId: {}", queue.getId(), e);
			}
		}
	}
}
