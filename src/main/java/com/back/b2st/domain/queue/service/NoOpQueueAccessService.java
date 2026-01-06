package com.back.b2st.domain.queue.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * queue.enabled=false 환경에서 대기열 검증을 스킵하는 구현체
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpQueueAccessService implements QueueAccessService {

	@Override
	public void assertEnterable(Long performanceId, Long userId) {
		log.debug("[QUEUE-OFF] skip assertEnterable - performanceId: {}, userId: {}", performanceId, userId);
	}
}
