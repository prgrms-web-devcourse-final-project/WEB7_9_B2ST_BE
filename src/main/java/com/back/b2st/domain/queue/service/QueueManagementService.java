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

	/**
	 * 대기열 생성
	 *
	 * @param request 대기열 생성 요청
	 * @return 생성된 대기열 정보
	 */
	@Transactional
	public QueueRes createQueue(CreateQueueReq request) {
		// 1. QueueType 검증
		QueueType queueType = validateQueueType(request.queueType());

		// 2. 중복 검증 (같은 scheduleId + queueType)
		validateNotDuplicated(request.scheduleId(), queueType);

		// 3. Queue 생성
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

	/**
	 * 대기열 단건 조회
	 *
	 * @param queueId 대기열 ID
	 * @return 대기열 정보 (Redis 실시간 정보 포함)
	 */
	public QueueRes getQueue(Long queueId) {
		// 1. DB 조회
		Queue queue = validateQueue(queueId);

		// 2. Redis 실시간 정보 조회 (Fallback)
		int currentWaiting = getRedisCountWithFallback(() ->
			queueRedisRepository.getTotalWaitingCount(queueId));
		// 현재 enterable 수 사용
		int currentEnterable = getRedisCountWithFallback(() ->
			queueRedisRepository.getTotalEnterableCount(queueId));

		return QueueRes.of(queue, currentWaiting, currentEnterable);
	}

	/**
	 * 회차별 대기열 목록 조회
	 *
	 * @param scheduleId 회차 ID
	 * @return 대기열 목록
	 */
	public List<QueueRes> getQueuesBySchedule(Long scheduleId) {
		List<Queue> queues = queueRepository.findByScheduleId(scheduleId);

		return queues.stream()
			.map(queue -> {
				// Redis 조회 (Fallback)
				int currentWaiting = getRedisCountWithFallback(() ->
					queueRedisRepository.getTotalWaitingCount(queue.getId()));
				int currentEnterable = getRedisCountWithFallback(() ->
					queueRedisRepository.getTotalEnterableCount(queue.getId()));

				return QueueRes.of(queue, currentWaiting, currentEnterable);
			})
			.collect(Collectors.toList());
	}

	/**
	 * 특정 타입의 대기열 목록 조회
	 *
	 * @param queueType 대기열 타입
	 * @return 대기열 목록
	 */
	public List<QueueRes> getQueuesByType(String queueType) {
		QueueType type = validateQueueType(queueType);
		List<Queue> queues = queueRepository.findByQueueType(type);

		return queues.stream()
			.map(QueueRes::from)
			.collect(Collectors.toList());
	}

	/**
	 * 대기열 설정 수정
	 *
	 * @param queueId 대기열 ID
	 * @param request 수정 요청
	 * @return 수정된 대기열 정보
	 */
	@Transactional
	public QueueRes updateQueue(Long queueId, UpdateQueueReq request) {
		// 1. Queue 조회
		Queue queue = validateQueue(queueId);

		// 2. 설정 업데이트
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

	/**
	 * 대기열 삭제
	 * Redis 데이터도 함께 삭제
	 *
	 * @param queueId 대기열 ID
	 */
	@Transactional
	public void deleteQueue(Long queueId) {
		// 1. Queue 존재 확인
		Queue queue = validateQueue(queueId);

		// 2. Redis 데이터 삭제
		// learAll()은 테스트 전용
		// 테스트 환경에서는 clearAll() 사용 가능 (queue.test.enabled=true)
		try {
			queueRedisRepository.clearAll(queueId);
		} catch (Exception e) {
			// clearAll()이 비활성화된 경우 개별 키 삭제
			log.warn("clearAll() 사용 불가, 개별 키 삭제 시도 - queueId: {}", queueId, e);
			// 개별 키 삭제는 보정 스케줄러가 처리하므로 여기서는 로그만 남김
		}

		// 3. DB 삭제
		try {
			queueRepository.delete(queue);
			log.info("Queue deleted - queueId: {}, scheduleId: {}", queueId, queue.getScheduleId());
		} catch (DataAccessException e) {
			log.error("Failed to delete queue - queueId: {}", queueId, e);
			throw new BusinessException(QueueErrorCode.QUEUE_INTERNAL_ERROR);
		}
	}

	/**
	 * 대기열 존재 여부 확인
	 *
	 * @param scheduleId 회차 ID
	 * @param queueType 대기열 타입
	 * @return 존재 여부
	 */
	public boolean existsQueue(Long scheduleId, String queueType) {
		QueueType type = validateQueueType(queueType);
		return queueRepository.existsByScheduleIdAndQueueType(scheduleId, type);
	}

	/**
	 * Queue 유효성 검증
	 */
	private Queue validateQueue(Long queueId) {
		return queueRepository.findById(queueId)
			.orElseThrow(() -> new BusinessException(QueueErrorCode.QUEUE_NOT_FOUND));
	}

	/**
	 * QueueType 검증
	 */
	private QueueType validateQueueType(String queueTypeStr) {
		try {
			return QueueType.valueOf(queueTypeStr.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new BusinessException(QueueErrorCode.INVALID_QUEUE_STATUS);
		}
	}

	/**
	 * 중복 대기열 검증
	 */
	private void validateNotDuplicated(Long scheduleId, QueueType queueType) {
		if (queueRepository.existsByScheduleIdAndQueueType(scheduleId, queueType)) {
			throw new BusinessException(QueueErrorCode.ALREADY_IN_QUEUE);
		}
	}

	/**
	 * Redis 카운트 조회 (Fallback 패턴)
	 *
	 * Redis 장애 시 0 반환
	 */
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

