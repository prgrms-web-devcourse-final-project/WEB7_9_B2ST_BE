package com.back.b2st.domain.queue.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.queue.dto.QueueDefaultPolicy;
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
		validateNotDuplicated(request.performanceId());

		Queue queue = Queue.builder()
			.performanceId(request.performanceId())
			.queueType(queueType)
			.maxActiveUsers(request.maxActiveUsers())
			.entryTtlMinutes(request.entryTtlMinutes())
			.build();

		try {
			queue = queueRepository.save(queue);
			log.info("Queue created - queueId: {}, performanceId: {}, type: {}",
				queue.getId(), queue.getPerformanceId(), queue.getQueueType());
			return QueueRes.from(queue);
		} catch (DataAccessException e) {
			log.error("Failed to create queue", e);
			throw new BusinessException(QueueErrorCode.QUEUE_INTERNAL_ERROR);
		}
	}

	/**
	 * 공연 ID로 대기열을 조회하거나 생성 (멱등성 보장)
	 *
	 * 레이스 컨디션 방어: 동시에 여러 요청이 와도 하나의 큐만 생성됨
	 *
	 * @param performanceId 공연 ID
	 * @param policy 기본 정책 (없을 때 생성 시 사용)
	 * @return 대기열 엔티티
	 */
	@Transactional
	public Queue getOrCreateByPerformanceId(Long performanceId, QueueDefaultPolicy policy) {
		// 1. 이미 존재하는지 확인
		return queueRepository.findByPerformanceId(performanceId)
			.orElseGet(() -> {
				log.info("Queue not found for performanceId: {}, creating new queue", performanceId);
				// 2. 생성 시도
				try {
					Queue newQueue = Queue.builder()
						.performanceId(performanceId)
						.queueType(policy.queueType())
						.maxActiveUsers(policy.maxActiveUsers())
						.entryTtlMinutes(policy.entryTtlMinutes())
						.build();

					Queue saved = queueRepository.save(newQueue);
					log.info("Queue created - queueId: {}, performanceId: {}", saved.getId(), performanceId);
					return saved;
				} catch (DataIntegrityViolationException e) {
					// 3. 레이스 컨디션: 다른 스레드가 이미 생성함
					log.debug("Race condition detected, queue already exists for performanceId: {}", performanceId);
					return queueRepository.findByPerformanceId(performanceId)
						.orElseThrow(() -> {
							log.error("Failed to find queue after race condition - performanceId: {}", performanceId);
							return new BusinessException(QueueErrorCode.QUEUE_INTERNAL_ERROR);
						});
				}
			});
	}

	public QueueRes getQueue(Long queueId) {
		Queue queue = validateQueue(queueId);

		int currentWaiting = getRedisCountWithFallback(() ->
			queueRedisRepository.getTotalWaitingCount(queueId));

		int currentEnterable = getRedisCountWithFallback(() ->
			queueRedisRepository.getTotalEnterableCount(queueId));

		return QueueRes.of(queue, currentWaiting, currentEnterable);
	}

	/**
	 * 공연 ID로 대기열 조회
	 *
	 * 공연당 큐는 1개만 존재하므로 (UNIQUE 제약), List로 반환하되 최대 1개만 포함
	 *
	 * @param performanceId 공연 ID
	 * @return 해당 공연의 대기열 목록 (최대 1개, Redis 실시간 카운트 포함)
	 */
	public List<QueueRes> getQueuesByPerformance(Long performanceId) {
		log.debug("Getting queues by performanceId: {}", performanceId);
		Optional<Queue> queueOpt = queueRepository.findByPerformanceId(performanceId);

		if (queueOpt.isEmpty()) {
			return List.of();
		}

		Queue queue = queueOpt.get();
		int currentWaiting = getRedisCountWithFallback(() ->
			queueRedisRepository.getTotalWaitingCount(queue.getId()));
		int currentEnterable = getRedisCountWithFallback(() ->
			queueRedisRepository.getTotalEnterableCount(queue.getId()));

		return List.of(QueueRes.of(queue, currentWaiting, currentEnterable));
	}

	/**
	 * @deprecated scheduleId 기반 조회는 더 이상 사용하지 않음. performanceId 기반 조회를 사용하세요.
	 */
	@Deprecated
	public List<QueueRes> getQueuesBySchedule(Long scheduleId) {
		// 더 이상 scheduleId로 조회할 수 없으므로 빈 리스트 반환
		log.warn("getQueuesBySchedule is deprecated. Use performanceId-based query instead. scheduleId: {}", scheduleId);
		return List.of();
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
			log.info("Queue deleted - queueId: {}, performanceId: {}", queueId, queue.getPerformanceId());
		} catch (DataAccessException e) {
			log.error("Failed to delete queue - queueId: {}", queueId, e);
			throw new BusinessException(QueueErrorCode.QUEUE_INTERNAL_ERROR);
		}
	}

	public boolean existsQueue(Long performanceId) {
		return queueRepository.existsByPerformanceId(performanceId);
	}

	/**
	 * @deprecated scheduleId 기반 조회는 더 이상 사용하지 않음. performanceId 기반 조회를 사용하세요.
	 */
	@Deprecated
	public boolean existsQueue(Long scheduleId, String queueType) {
		log.warn("existsQueue(scheduleId, queueType) is deprecated. Use existsQueue(performanceId) instead.");
		return false;
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

	private void validateNotDuplicated(Long performanceId) {
		if (queueRepository.existsByPerformanceId(performanceId)) {
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
