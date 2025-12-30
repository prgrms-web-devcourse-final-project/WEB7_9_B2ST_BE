package com.back.b2st.domain.queue.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.queue.dto.response.QueueEntryRes;
import com.back.b2st.domain.queue.dto.response.QueueStatusRes;
import com.back.b2st.domain.queue.entity.Queue;
import com.back.b2st.domain.queue.entity.QueueEntry;
import com.back.b2st.domain.queue.entity.QueueEntryStatus;
import com.back.b2st.domain.queue.error.QueueErrorCode;
import com.back.b2st.domain.queue.repository.QueueEntryRepository;
import com.back.b2st.domain.queue.repository.QueueRedisRepository;
import com.back.b2st.domain.queue.repository.QueueRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Queue Service
 *
 * 대기열 시스템의 핵심 비즈니스 로직
 * - Redis: 실시간 순번 관리
 * - PostgreSQL: 영구 저장 및 통계
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
@Transactional(readOnly = true)
public class QueueService {

	private final QueueRepository queueRepository;
	private final QueueEntryRepository queueEntryRepository;
	private final QueueRedisRepository queueRedisRepository;

	/**
	 * 대기열 입장
	 *
	 * 1. 대기열 유효성 검증
	 * 2. 중복 입장 체크
	 * 3. Redis ZSET 추가 (실시간 순번 관리)
	 *
	 * @param queueId 대기열 ID
	 * @param userId 사용자 ID
	 * @return 입장 결과 (순번, 앞사람 수 포함)
	 */
	@Transactional
	public QueueEntryRes enterQueue(Long queueId, Long userId) {
		// 1. 대기열 유효성 검증
		Queue queue = validateQueue(queueId);

		// 2. 중복 입장 체크
		validateNotDuplicated(queueId, userId);

		// 3. Redis에 추가 (실시간 순번 관리)
		long timestamp = Instant.now().toEpochMilli();
		queueRedisRepository.addToWaitingQueue(queueId, userId, (int)timestamp);

		// 4. 현재 순번 조회
		Long myRank = queueRedisRepository.getMyRankInWaiting(queueId, userId);
		Long waitingAhead = queueRedisRepository.getWaitingAheadCount(queueId, userId);

		log.info("User entered queue (Redis only) - queueId: {}, userId: {}, rank: {}", queueId, userId, myRank);

		// 5. WAITING 상태 응답 (DB 저장 없이 Redis 정보만 반환)
		return QueueEntryRes.waiting(
			queueId,
			userId,
			myRank != null ? myRank.intValue() : 0,
			waitingAhead != null ? waitingAhead.intValue() : 0
		);
	}

	/**
	 * 내 대기 상태 조회
	 *
	 * Redis 우선 조회 (실시간 순번) + DB Fallback
	 * Switch 표현식으로 상태별 처리
	 *
	 * @param queueId 대기열 ID
	 * @param userId 사용자 ID
	 * @return 현재 상태
	 */
	public QueueStatusRes getMyStatus(Long queueId, Long userId) {
		// 1. 대기열 유효성 검증
		validateQueue(queueId);

		// 2. Redis 우선 조회 (Fallback 포함)
		try {
			// Redis WAITING 체크
			if (queueRedisRepository.isInWaitingQueue(queueId, userId)) {
				return buildWaitingResponse(queueId, userId);
			}

			// Redis ENTERABLE 체크
			if (queueRedisRepository.isInEnterable(queueId, userId)) {
				return QueueStatusRes.enterable(queueId, userId);
			}
		} catch (Exception e) {
			log.warn("Redis 조회 실패, DB Fallback - queueId: {}, userId: {}", queueId, userId, e);
		}

		// 3. DB 조회
		QueueEntry entry = queueEntryRepository.findByQueueIdAndUserId(queueId, userId)
			.orElseThrow(() -> new BusinessException(QueueErrorCode.NOT_IN_QUEUE));

		// 4. Switch 표현식으로 상태별 처리
		return buildResponseByStatus(queueId, userId, entry);
	}

	/**
	 * WAITING 상태 응답 빌더
	 */
	private QueueStatusRes buildWaitingResponse(Long queueId, Long userId) {
		try {
			Long myRank = queueRedisRepository.getMyRankInWaiting(queueId, userId);
			Long waitingAhead = queueRedisRepository.getWaitingAheadCount(queueId, userId);
			Long totalWaiting = queueRedisRepository.getTotalWaitingCount(queueId);

			return QueueStatusRes.waiting(
				queueId,
				userId,
				myRank != null ? myRank.intValue() : 0,
				waitingAhead != null ? waitingAhead.intValue() : 0,
				totalWaiting != null ? totalWaiting.intValue() : 0
			);
		} catch (Exception e) {
			log.warn("Redis WAITING 정보 조회 실패 - queueId: {}, userId: {}", queueId, userId, e);
			throw e;
		}
	}

	/**
	 * DB 상태 기반 응답 빌더
	 */
	private QueueStatusRes buildResponseByStatus(Long queueId, Long userId, QueueEntry entry) {
		return switch (entry.getStatus()) {
			case ENTERABLE -> QueueStatusRes.enterable(queueId, userId);
			case COMPLETED, EXPIRED -> QueueStatusRes.fromEntry(entry);
		};
	}

	/**
	 * 대기 중인 사용자를 입장 가능 상태로 이동
	 *
	 * Scheduler 또는 Admin에서 호출
	 * 1. Redis: WAITING → ENTERABLE (TTL 적용) -먼저실행
	 * 2. DB: 처음으로 ENTERABLE 상태로 저장 (expiresAt 설정)
	 *
	 * @param queueId 대기열 ID
	 * @param userId 사용자 ID
	 */
	@Transactional
	public void moveToEnterable(Long queueId, Long userId) {
		// 1. Queue 조회
		Queue queue = validateQueue(queueId);

		// 2. Redis 상태 이동
		boolean redisSuccess = false;
		try {
			queueRedisRepository.moveToEnterable(queueId, userId, queue.getEntryTtlMinutes());
			redisSuccess = true;
		} catch (Exception e) {
			log.error("Redis 이동 실패, 트랜잭션 롤백 - queueId: {}, userId: {}", queueId, userId, e);
			throw new BusinessException(QueueErrorCode.REDIS_OPERATION_FAILED);
		}

		// 3. 누적 카운트 증가
		try {
			queueRedisRepository.incrementEnterableCount(queueId);
		} catch (Exception e) {
			log.warn("Redis 카운트 증가 실패 (비중요) - queueId: {}", queueId, e);
		}

		// 4. DB에 ENTERABLE 상태로 처음 저장
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime expiresAt = now.plusMinutes(queue.getEntryTtlMinutes());

		QueueEntry entry = QueueEntry.builder()
			.queueId(queueId)
			.userId(userId)
			.joinedAt(now)
			.enterableAt(now)
			.expiresAt(expiresAt)
			.build();  // status는 자동으로 ENTERABLE

		try {
			queueEntryRepository.save(entry);
			log.info("User moved to enterable (Redis→DB) - queueId: {}, userId: {}, expiresAt: {}",
				queueId, userId, expiresAt);
		} catch (DataAccessException e) {
			log.error("DB 저장 실패, Redis 롤백 시도 - queueId: {}, userId: {}", queueId, userId, e);

			if (redisSuccess) {
				try {
					// Redis 롤백: ENTERABLE → WAITING
					queueRedisRepository.rollbackToWaiting(queueId, userId);
					log.info("Redis 롤백 완료 - queueId: {}, userId: {}", queueId, userId);
				} catch (Exception rollbackException) {
					log.error("Redis 롤백 실패 - queueId: {}, userId: {}", queueId, userId, rollbackException);
					// 롤백 실패해도 예외는 DB 저장 실패 예외로 전달
				}
			}

			throw new BusinessException(QueueErrorCode.QUEUE_INTERNAL_ERROR);
		}
	}

	/**
	 * 입장 완료 처리
	 *
	 * @param queueId 대기열 ID
	 * @param userId 사용자 ID
	 */
	@Transactional
	public void completeEntry(Long queueId, Long userId) {
		// 1. 대기열 검증
		validateQueue(queueId);

		// 2. Redis 토큰 검증 (Redis 토큰 우선)
		if (!queueRedisRepository.isInEnterable(queueId, userId)) {
			log.warn("Redis 토큰 없음으로 입장 완료 거부 - queueId: {}, userId: {}", queueId, userId);
			throw new BusinessException(QueueErrorCode.INVALID_QUEUE_STATUS);
		}

		// 3. DB 엔트리 검증
		QueueEntry entry = validateQueueEntry(queueId, userId);

		// 4. DB 상태 검증
		if (entry.getStatus() != QueueEntryStatus.ENTERABLE) {
			throw new BusinessException(QueueErrorCode.INVALID_QUEUE_STATUS);
		}

		// 5. 만료 시간 확인 (추가 안전장치)
		if (entry.getExpiresAt() != null && entry.getExpiresAt().isBefore(LocalDateTime.now())) {
			log.warn("만료된 입장권으로 입장 완료 거부 - queueId: {}, userId: {}, expiresAt: {}",
				queueId, userId, entry.getExpiresAt());
			throw new BusinessException(QueueErrorCode.INVALID_QUEUE_STATUS);
		}

		// 6. DB 상태 업데이트
		entry.updateToCompleted(LocalDateTime.now());

		try {
			queueEntryRepository.save(entry);
		} catch (DataAccessException e) {
			throw new BusinessException(QueueErrorCode.QUEUE_INTERNAL_ERROR);
		}

		// 7. Redis에서 제거
		queueRedisRepository.removeFromEnterable(queueId, userId);

		log.info("User completed entry - queueId: {}, userId: {}", queueId, userId);
	}

	/**
	 * 대기열 나가기
	 *
	 * 사용자가 대기를 포기했을 때 호출
	 * - WAITING 상태: Redis에서만 제거 (DB 기록 없음)
	 * - ENTERABLE 상태: Redis 제거 + DB 상태 EXPIRED로 변경
	 *
	 * @param queueId 대기열 ID
	 * @param userId 사용자 ID
	 */
	@Transactional
	public void exitQueue(Long queueId, Long userId) {
		// 1. 대기열 검증
		validateQueue(queueId);

		// 2. Redis WAITING 상태 체크
		if (queueRedisRepository.isInWaitingQueue(queueId, userId)) {
			// WAITING 상태: Redis에서만 제거
			queueRedisRepository.removeFromWaitingQueue(queueId, userId);
			log.info("User exited queue (WAITING, Redis only) - queueId: {}, userId: {}", queueId, userId);
			return;
		}

		// 3. Redis ENTERABLE 상태 체크
		if (queueRedisRepository.isInEnterable(queueId, userId)) {
			// ENTERABLE 상태: Redis 제거 + DB 상태 변경
			queueRedisRepository.removeFromEnterable(queueId, userId);

			// DB 엔트리 조회 및 상태 변경
			Optional<QueueEntry> entryOpt = queueEntryRepository.findByQueueIdAndUserId(queueId, userId);
			if (entryOpt.isPresent()) {
				QueueEntry entry = entryOpt.get();
				entry.updateToExpired();

				try {
					queueEntryRepository.save(entry);
				} catch (DataAccessException e) {
					throw new BusinessException(QueueErrorCode.QUEUE_INTERNAL_ERROR);
				}
			}

			log.info("User exited queue (ENTERABLE) - queueId: {}, userId: {}", queueId, userId);
			return;
		}

		// 4. 대기열에 없음
		throw new BusinessException(QueueErrorCode.NOT_IN_QUEUE);
	}

	/**
	 * 대기열 통계 조회
	 *
	 * @param queueId 대기열 ID
	 * @return 대기 중, 입장 가능, 완료 등의 통계
	 */
	public QueueStatusRes getQueueStatistics(Long queueId) {
		// 1. Queue 조회
		Queue queue = validateQueue(queueId);

		// 2. Redis 실시간 데이터
		Long totalWaiting = queueRedisRepository.getTotalWaitingCount(queueId);
		Long totalEnterable = queueRedisRepository.getTotalEnterableCount(queueId);

		// 3. 통계 반환
		return QueueStatusRes.statistics(
			queueId,
			totalWaiting != null ? totalWaiting.intValue() : 0,
			totalEnterable != null ? totalEnterable.intValue() : 0,
			queue.getMaxActiveUsers()
		);
	}

	/**
	 * 대기열 입장 가능 인원 확인
	 *
	 * @param queueId 대기열 ID
	 * @return 입장 가능 여부
	 */
	public boolean canEnterMore(Long queueId) {
		Queue queue = validateQueue(queueId);

		Long currentEnterable = queueRedisRepository.getTotalEnterableCount(queueId);
		int current = currentEnterable != null ? currentEnterable.intValue() : 0;

		return current < queue.getMaxActiveUsers();
	}

	/**
	 * 다음 입장 허용 인원 계산
	 *
	 * @param queueId 대기열 ID
	 * @return 입장 가능한 인원 수
	 */
	public int getAvailableSlots(Long queueId) {
		Queue queue = validateQueue(queueId);

		Long currentEnterable = queueRedisRepository.getTotalEnterableCount(queueId);
		int current = currentEnterable != null ? currentEnterable.intValue() : 0;

		return Math.max(0, queue.getMaxActiveUsers() - current);
	}

	/**
	 * 대기열 유효성 검증
	 */
	private Queue validateQueue(Long queueId) {
		return queueRepository.findById(queueId)
			.orElseThrow(() -> new BusinessException(QueueErrorCode.QUEUE_NOT_FOUND));
	}

	/**
	 * 대기열 엔트리 검증
	 */
	private QueueEntry validateQueueEntry(Long queueId, Long userId) {
		return queueEntryRepository.findByQueueIdAndUserId(queueId, userId)
			.orElseThrow(() -> new BusinessException(QueueErrorCode.NOT_IN_QUEUE));
	}

	/**
	 * 중복 입장 검증
	 * - Redis WAITING 체크
	 * - Redis ENTERABLE 체크
	 * - DB에서 ENTERABLE, COMPLETED 상태 체크
	 */
	private void validateNotDuplicated(Long queueId, Long userId) {
		// 1. Redis WAITING 체크
		if (queueRedisRepository.isInWaitingQueue(queueId, userId)) {
			throw new BusinessException(QueueErrorCode.ALREADY_IN_QUEUE);
		}

		// 2. Redis ENTERABLE 체크
		if (queueRedisRepository.isInEnterable(queueId, userId)) {
			throw new BusinessException(QueueErrorCode.ALREADY_IN_QUEUE);
		}

		// 3. DB에서 ENTERABLE 또는 COMPLETED 상태 체크
		Optional<QueueEntry> existingEntry = queueEntryRepository.findByQueueIdAndUserId(queueId, userId);
		if (existingEntry.isPresent()) {
			QueueEntry entry = existingEntry.get();
			if (entry.getStatus() == QueueEntryStatus.ENTERABLE ||
				entry.getStatus() == QueueEntryStatus.COMPLETED) {
				throw new BusinessException(QueueErrorCode.ALREADY_IN_QUEUE);
			}
			// EXPIRED 상태는 재입장 가능 (체크하지 않음)
		}
	}
}



