package com.back.b2st.domain.queue.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.back.b2st.domain.queue.entity.Queue;
import com.back.b2st.domain.queue.error.QueueErrorCode;
import com.back.b2st.domain.queue.repository.QueueRedisRepository;
import com.back.b2st.domain.queue.repository.QueueRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 대기열 접근 제어 서비스 (queue.enabled=true 일 때만 활성)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
public class QueueAccessServiceImpl implements QueueAccessService {

	private final QueueRepository queueRepository;
	private final QueueRedisRepository queueRedisRepository;

	/**
	 * (내부용) 사용자가 해당 공연의 대기열을 통과했는지 확인
	 *
	 * @param performanceId 공연 ID
	 * @param userId 사용자 ID
	 * @return ENTERABLE 상태면 true, 아니면 false
	 */
	public boolean isEnterable(Long performanceId, Long userId) {
		// 1. 공연의 큐가 존재하는지 확인
		Queue queue = queueRepository.findByPerformanceId(performanceId)
			.orElse(null);

		if (queue == null) {
			log.debug("Queue not found for performanceId: {}", performanceId);
			return false;
		}

		// 2. Redis에서 ENTERABLE 상태 확인 (SoT)
		try {
			boolean enterable = queueRedisRepository.isInEnterable(queue.getId(), userId);
			log.debug("isEnterable check - performanceId: {}, userId: {}, queueId: {}, result: {}",
				performanceId, userId, queue.getId(), enterable);
			return enterable;
		} catch (Exception e) {
			log.warn("Redis operation failed in isEnterable - performanceId: {}, userId: {}", performanceId, userId, e);
			return false;
		}
	}

	/**
	 * 사용자가 해당 공연의 대기열을 통과했는지 확인 (권장)
	 *
	 * ENTERABLE 상태가 아니면 BusinessException throw
	 */
	@Override
	public void assertEnterable(Long performanceId, Long userId) {
		if (!isEnterable(performanceId, userId)) {
			log.warn("User not enterable - performanceId: {}, userId: {}", performanceId, userId);
			throw new BusinessException(QueueErrorCode.QUEUE_ENTRY_EXPIRED);
		}
		log.debug("User enterable verified - performanceId: {}, userId: {}", performanceId, userId);
	}
}
