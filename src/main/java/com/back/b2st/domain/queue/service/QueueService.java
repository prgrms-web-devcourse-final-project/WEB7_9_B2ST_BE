package com.back.b2st.domain.queue.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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
 * - Redis: 실시간 순번 관리 + ENTERABLE 토큰(TTL) 발급/검증 (SoT)
 * - PostgreSQL: 영구 저장(감사/통계/정산) + 상태 기록
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
	 * - WAITING은 Redis만 사용
	 * - DB에는 WAITING을 저장하지 않는다
	 */
	@Transactional
	public QueueEntryRes enterQueue(Long queueId, Long userId) {
		validateQueue(queueId);
		validateNotDuplicated(queueId, userId);

		long timestamp = Instant.now().toEpochMilli();
		queueRedisRepository.addToWaitingQueue(queueId, userId, timestamp);

		Long myRank = queueRedisRepository.getMyRankInWaiting(queueId, userId);
		Long waitingAhead = queueRedisRepository.getWaitingAheadCount(queueId, userId);

		log.info("User entered queue (WAITING, Redis only) - queueId: {}, userId: {}, rank: {}",
			queueId, userId, myRank);

		return QueueEntryRes.waiting(
			queueId,
			userId,
			myRank != null ? myRank.intValue() : 0,
			waitingAhead != null ? waitingAhead.intValue() : 0
		);
	}

	/**
	 * 내 대기 상태 조회
	 */
	public QueueStatusRes getMyStatus(Long queueId, Long userId) {
		validateQueue(queueId);

		// 1) Redis 기반 상태 조회
		try {
			if (queueRedisRepository.isInWaitingQueue(queueId, userId)) {
				return buildWaitingResponse(queueId, userId);
			}
			if (queueRedisRepository.isInEnterable(queueId, userId)) {
				return QueueStatusRes.enterable(queueId, userId);
			}
		} catch (Exception e) {
			log.warn("Redis 조회 실패 - queueId: {}, userId: {}", queueId, userId, e);
			throw new BusinessException(QueueErrorCode.REDIS_OPERATION_FAILED);
		}

		// 2) Redis에 없으면 DB로 상태를 보여줄 수는 있음 ㄷ(완료/만료 등 기록)
		//    단, ENTERABLE 판정은 절대 DB로 하지 않는다.
		QueueEntry entry = queueEntryRepository.findByQueueIdAndUserId(queueId, userId)
			.orElseThrow(() -> new BusinessException(QueueErrorCode.NOT_IN_QUEUE));

		return buildResponseByStatus(queueId, userId, entry);
	}

	private QueueStatusRes buildWaitingResponse(Long queueId, Long userId) {
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
	}

	private QueueStatusRes buildResponseByStatus(Long queueId, Long userId, QueueEntry entry) {
		return switch (entry.getStatus()) {
			case ENTERABLE -> QueueStatusRes.fromEntry(entry); // "DB 기록상 ENTERABLE"
			case COMPLETED, EXPIRED -> QueueStatusRes.fromEntry(entry);
		};
	}

	/**
	 * WAITING → ENTERABLE 전환 (스케줄러 호출)
	 */
	@Transactional
	public void moveToEnterable(Long queueId, Long userId) {
		Queue queue = validateQueue(queueId);

		// 1) Redis 원자적 이동
		boolean moved;
		try {
			moved = queueRedisRepository.moveToEnterable(queueId, userId, queue.getEntryTtlMinutes());
		} catch (Exception e) {
			log.error("Redis moveToEnterable 실패 - queueId: {}, userId: {}", queueId, userId, e);
			throw new BusinessException(QueueErrorCode.REDIS_OPERATION_FAILED);
		}

		if (!moved) {
			log.debug("moveToEnterable skipped (already processed / not in WAITING) - queueId: {}, userId: {}",
				queueId, userId);
			return;
		}

		// 2) DB upsert
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime expiresAt = now.plusMinutes(queue.getEntryTtlMinutes());

		try {
			QueueEntry entry = queueEntryRepository.findByQueueIdAndUserId(queueId, userId)
				.orElseGet(() -> QueueEntry.builder()
					.queueId(queueId)
					.userId(userId)
					.joinedAt(now)
					.enterableAt(now)
					.expiresAt(expiresAt)
					.build()
				);

			// 재진입/재발급 시 토큰을 명시적으로 새로 부여
			entry.updateToEnterable(
				UUID.randomUUID(),
				now,
				now,
				expiresAt
			);

			queueEntryRepository.save(entry);

		} catch (DataAccessException e) {
			// 3) DB 실패 시 Redis 보상 트랜잭션
			log.error("DB 저장 실패, Redis 롤백 시도 - queueId: {}, userId: {}", queueId, userId, e);
			try {
				queueRedisRepository.rollbackToWaiting(queueId, userId);
			} catch (Exception rollbackException) {
				log.error("Redis 롤백 실패(치명) - queueId: {}, userId: {}", queueId, userId, rollbackException);
			}
			throw new BusinessException(QueueErrorCode.QUEUE_INTERNAL_ERROR);
		}

		// 누적 카운트
		try {
			queueRedisRepository.incrementEnterableCount(queueId);
		} catch (Exception e) {
			log.warn("Redis 누적 카운트 증가 실패(비중요) - queueId: {}", queueId, e);
		}

		log.info("User moved to ENTERABLE (Redis→DB) - queueId: {}, userId: {}, expiresAt: {}",
			queueId, userId, expiresAt);
	}

	/**
	 * 입장 완료 처리
	 */
	@Transactional
	public void completeEntry(Long queueId, Long userId) {
		validateQueue(queueId);

		// 1) Redis 토큰 검증
		boolean hasToken;
		try {
			hasToken = queueRedisRepository.isInEnterable(queueId, userId);
		} catch (Exception e) {
			log.warn("Redis 토큰 검증 실패 - queueId: {}, userId: {}", queueId, userId, e);
			throw new BusinessException(QueueErrorCode.REDIS_OPERATION_FAILED);
		}

		if (!hasToken) {
			log.warn("Redis 토큰 없음으로 입장 완료 거부 - queueId: {}, userId: {}", queueId, userId);
			throw new BusinessException(QueueErrorCode.INVALID_QUEUE_STATUS);
		}

		// 2) DB 검증
		QueueEntry entry = validateQueueEntry(queueId, userId);
		if (entry.getStatus() != QueueEntryStatus.ENTERABLE) {
			throw new BusinessException(QueueErrorCode.INVALID_QUEUE_STATUS);
		}

		LocalDateTime now = LocalDateTime.now();
		if (entry.getExpiresAt() != null && entry.getExpiresAt().isBefore(now)) {
			log.warn("DB 기준 만료(기록) 확인 - queueId: {}, userId: {}, expiresAt: {}",
				queueId, userId, entry.getExpiresAt());
			throw new BusinessException(QueueErrorCode.INVALID_QUEUE_STATUS);
		}

		// 3) DB 완료 처리
		entry.updateToCompleted(now);
		try {
			queueEntryRepository.save(entry);
		} catch (DataAccessException e) {
			throw new BusinessException(QueueErrorCode.QUEUE_INTERNAL_ERROR);
		}

		// 4) Redis 제거
		try {
			queueRedisRepository.removeFromEnterable(queueId, userId);
		} catch (Exception e) {
			log.warn("Redis 제거 실패(비중요) - queueId: {}, userId: {}", queueId, userId, e);
		}

		log.info("User completed entry - queueId: {}, userId: {}", queueId, userId);
	}

	/**
	 * 대기열 나가기(취소)
	 */
	@Transactional
	public void exitQueue(Long queueId, Long userId) {
		validateQueue(queueId);

		// 1) WAITING이면 Redis에서 제거
		boolean inWaiting;
		try {
			inWaiting = queueRedisRepository.isInWaitingQueue(queueId, userId);
		} catch (Exception e) {
			log.warn("Redis WAITING 확인 실패 - queueId: {}, userId: {}", queueId, userId, e);
			throw new BusinessException(QueueErrorCode.REDIS_OPERATION_FAILED);
		}

		if (inWaiting) {
			try {
				queueRedisRepository.removeFromWaitingQueue(queueId, userId);
			} catch (Exception e) {
				log.warn("Redis WAITING 제거 실패 - queueId: {}, userId: {}", queueId, userId, e);
				throw new BusinessException(QueueErrorCode.REDIS_OPERATION_FAILED);
			}
			log.info("User exited queue (WAITING, Redis only) - queueId: {}, userId: {}", queueId, userId);
			return;
		}

		// 2) ENTERABLE은 토큰으로만 판정
		boolean inEnterable;
		try {
			inEnterable = queueRedisRepository.isInEnterable(queueId, userId);
		} catch (Exception e) {
			log.warn("Redis ENTERABLE 토큰 확인 실패 - queueId: {}, userId: {}", queueId, userId, e);
			throw new BusinessException(QueueErrorCode.REDIS_OPERATION_FAILED);
		}

		if (!inEnterable) {
			throw new BusinessException(QueueErrorCode.NOT_IN_QUEUE);
		}

		// 3) Redis 제거
		try {
			queueRedisRepository.removeFromEnterable(queueId, userId);
		} catch (Exception e) {
			log.warn("Redis ENTERABLE 제거 실패 - queueId: {}, userId: {}", queueId, userId, e);
			throw new BusinessException(QueueErrorCode.REDIS_OPERATION_FAILED);
		}

		// 4) DB 상태 EXPIRED (있으면 업데이트)
		Optional<QueueEntry> entryOpt = queueEntryRepository.findByQueueIdAndUserId(queueId, userId);
		if (entryOpt.isPresent()) {
			QueueEntry entry = entryOpt.get();
			if (entry.getStatus() == QueueEntryStatus.ENTERABLE) {
				entry.updateToExpired();
				try {
					queueEntryRepository.save(entry);
				} catch (DataAccessException e) {
					throw new BusinessException(QueueErrorCode.QUEUE_INTERNAL_ERROR);
				}
			}
		}

		log.info("User exited queue (ENTERABLE) - queueId: {}, userId: {}", queueId, userId);
	}

	/**
	 * 대기열 통계 조회
	 */
	public QueueStatusRes getQueueStatistics(Long queueId) {
		Queue queue = validateQueue(queueId);

		Long totalWaiting = queueRedisRepository.getTotalWaitingCount(queueId);

		Long totalEnterable = queueRedisRepository.getTotalEnterableCount(queueId);

		return QueueStatusRes.statistics(
			queueId,
			totalWaiting != null ? totalWaiting.intValue() : 0,
			totalEnterable != null ? totalEnterable.intValue() : 0,
			queue.getMaxActiveUsers()
		);
	}

	/**
	 * 대기열 입장 가능 인원 확인
	 */
	public boolean canEnterMore(Long queueId) {
		Queue queue = validateQueue(queueId);

		Long currentEnterable = queueRedisRepository.getTotalEnterableCount(queueId);
		int current = currentEnterable != null ? currentEnterable.intValue() : 0;

		return current < queue.getMaxActiveUsers();
	}

	/**
	 * 다음 입장 허용 인원 계산
	 */
	public int getAvailableSlots(Long queueId) {
		Queue queue = validateQueue(queueId);

		Long currentEnterable = queueRedisRepository.getTotalEnterableCount(queueId);
		int current = currentEnterable != null ? currentEnterable.intValue() : 0;

		int availableSlots = Math.max(0, queue.getMaxActiveUsers() - current);

		if (current > queue.getMaxActiveUsers()) {
			log.warn("currentEnterable이 maxActiveUsers 초과(정합성 이슈) - queueId: {}, current: {}, max: {}",
				queueId, current, queue.getMaxActiveUsers());
		}

		return availableSlots;
	}

	/* ==================== Validation ==================== */

	private Queue validateQueue(Long queueId) {
		return queueRepository.findById(queueId)
			.orElseThrow(() -> new BusinessException(QueueErrorCode.QUEUE_NOT_FOUND));
	}

	private QueueEntry validateQueueEntry(Long queueId, Long userId) {
		return queueEntryRepository.findByQueueIdAndUserId(queueId, userId)
			.orElseThrow(() -> new BusinessException(QueueErrorCode.NOT_IN_QUEUE));
	}

	/**
	 * 중복 입장 검증
	 * - Redis WAITING 체크
	 * - Redis ENTERABLE(토큰) 체크
	 * - DB에서 ENTERABLE/COMPLETED 체크
	 */
	private void validateNotDuplicated(Long queueId, Long userId) {
		if (queueRedisRepository.isInWaitingQueue(queueId, userId)) {
			throw new BusinessException(QueueErrorCode.ALREADY_IN_QUEUE);
		}

		if (queueRedisRepository.isInEnterable(queueId, userId)) {
			throw new BusinessException(QueueErrorCode.ALREADY_IN_QUEUE);
		}

		Optional<QueueEntry> existingEntry = queueEntryRepository.findByQueueIdAndUserId(queueId, userId);
		if (existingEntry.isPresent()) {
			QueueEntry entry = existingEntry.get();
			if (entry.getStatus() == QueueEntryStatus.ENTERABLE ||
				entry.getStatus() == QueueEntryStatus.COMPLETED) {
				throw new BusinessException(QueueErrorCode.ALREADY_IN_QUEUE);
			}
			// EXPIRED는 재입장 가능
		}
	}
}
