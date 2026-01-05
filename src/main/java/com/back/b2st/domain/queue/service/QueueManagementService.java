package com.back.b2st.domain.queue.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.queue.dto.request.CreateQueueReq;
import com.back.b2st.domain.queue.dto.request.UpdateQueueReq;
import com.back.b2st.domain.queue.dto.response.QueueRes;
import com.back.b2st.domain.queue.entity.Queue;
import com.back.b2st.domain.queue.entity.QueueType;
import com.back.b2st.domain.queue.error.QueueErrorCode;
import com.back.b2st.domain.queue.repository.QueueRedisRepository;
import com.back.b2st.domain.queue.repository.QueueRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Queue 관리 서비스
 *
 * Queue 엔티티 생성/조회/수정/삭제 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
@Transactional(readOnly = true)
public class QueueManagementService {

	private final QueueRepository queueRepository;
	private final QueueRedisRepository queueRedisRepository;

	@Transactional
	public QueueRes createQueue(CreateQueueReq request) {
		QueueType queueType = validateQueueType(request.queueType());
		validateNotDuplicated(request.scheduleId(), queueType);

		Queue queue = Queue.builder()
			.scheduleId(request.scheduleId())
			.queueType(queueType)
			.maxActiveUsers(request.maxActiveUsers())
			.entryTtlMinutes(request.entryTtlMinutes())
			.build();

		try {
			queue = queueRepository.save(queue);
			log.info("Queue created - queueId: {}, scheduleId: {}, type: {}",
				queue.getId(), queue.getScheduleId(), queue.getQueueType());
			return QueueRes.from(queue);
		} catch (DataAccessException e) {
			log.error("Failed to create queue", e);
			throw new BusinessException(QueueErrorCode.QUEUE_INTERNAL_ERROR);
		}
	}

	public QueueRes getQueue(Long queueId) {
		Queue queue = validateQueue(queueId);

		int currentWaiting = getRedisCountWithFallback(() ->
			queueRedisRepository.getTotalWaitingCount(queueId));

		int currentEnterable = getRedisCountWithFallback(() ->
			queueRedisRepository.getTotalEnterableCount(queueId));

		return QueueRes.of(queue, currentWaiting, currentEnterable);
	}

	public List<QueueRes> getQueuesBySchedule(Long scheduleId) {
		List<Queue> queues = queueRepository.findByScheduleId(scheduleId);

		return queues.stream()
			.map(queue -> {
				int currentWaiting = getRedisCountWithFallback(() ->
					queueRedisRepository.getTotalWaitingCount(queue.getId()));
				int currentEnterable = getRedisCountWithFallback(() ->
					queueRedisRepository.getTotalEnterableCount(queue.getId()));
				return QueueRes.of(queue, currentWaiting, currentEnterable);
			})
			.collect(Collectors.toList());
	}

	public List<QueueRes> getQueuesByType(String queueType) {
		QueueType type = validateQueueType(queueType);
		List<Queue> queues = queueRepository.findByQueueType(type);

		return queues.stream()
			.map(QueueRes::from)
			.collect(Collectors.toList());
	}

	/**
	 * 전체 대기열 목록 조회
	 *
	 * @return 모든 대기열 목록 (Redis 실시간 카운트 포함)
	 */
	public List<QueueRes> getAllQueues() {
		List<Queue> queues = queueRepository.findAll();

		return queues.stream()
			.map(queue -> {
				int currentWaiting = getRedisCountWithFallback(() ->
					queueRedisRepository.getTotalWaitingCount(queue.getId()));
				int currentEnterable = getRedisCountWithFallback(() ->
					queueRedisRepository.getTotalEnterableCount(queue.getId()));
				return QueueRes.of(queue, currentWaiting, currentEnterable);
			})
			.collect(Collectors.toList());
	}

	@Transactional
	public QueueRes updateQueue(Long queueId, UpdateQueueReq request) {
		Queue queue = validateQueue(queueId);

		if (request.maxActiveUsers() != null) {
			queue.updateMaxActiveUsers(request.maxActiveUsers());
		}
		if (request.entryTtlMinutes() != null) {
			queue.updateEntryTtl(request.entryTtlMinutes());
		}

		try {
			queue = queueRepository.save(queue);
			log.info("Queue updated - queueId: {}, maxActiveUsers: {}, entryTtlMinutes: {}",
				queueId, queue.getMaxActiveUsers(), queue.getEntryTtlMinutes());
			return QueueRes.from(queue);
		} catch (DataAccessException e) {
			log.error("Failed to update queue - queueId: {}", queueId, e);
			throw new BusinessException(QueueErrorCode.QUEUE_INTERNAL_ERROR);
		}
	}

	@Transactional
	public void deleteQueue(Long queueId) {
		Queue queue = validateQueue(queueId);

		try {
			queueRedisRepository.clearAll(queueId);
		} catch (Exception e) {
			log.warn("clearAll() 사용 불가 또는 실패, 개별 키 정리는 스케줄러/운영 정책에 위임 - queueId: {}", queueId, e);
		}

		try {
			queueRepository.delete(queue);
			log.info("Queue deleted - queueId: {}, scheduleId: {}", queueId, queue.getScheduleId());
		} catch (DataAccessException e) {
			log.error("Failed to delete queue - queueId: {}", queueId, e);
			throw new BusinessException(QueueErrorCode.QUEUE_INTERNAL_ERROR);
		}
	}

	public boolean existsQueue(Long scheduleId, String queueType) {
		QueueType type = validateQueueType(queueType);
		return queueRepository.existsByScheduleIdAndQueueType(scheduleId, type);
	}

	private Queue validateQueue(Long queueId) {
		return queueRepository.findById(queueId)
			.orElseThrow(() -> new BusinessException(QueueErrorCode.QUEUE_NOT_FOUND));
	}

	private QueueType validateQueueType(String queueTypeStr) {
		try {
			return QueueType.valueOf(queueTypeStr.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new BusinessException(QueueErrorCode.INVALID_QUEUE_STATUS);
		}
	}

	private void validateNotDuplicated(Long scheduleId, QueueType queueType) {
		if (queueRepository.existsByScheduleIdAndQueueType(scheduleId, queueType)) {
			throw new BusinessException(QueueErrorCode.ALREADY_IN_QUEUE);
		}
	}

	private int getRedisCountWithFallback(java.util.function.Supplier<Long> redisOperation) {
		try {
			Long count = redisOperation.get();
			return count != null ? count.intValue() : 0;
		} catch (Exception e) {
			log.warn("Redis 조회 실패, 0 반환", e);
			return 0;
		}
	}
}
