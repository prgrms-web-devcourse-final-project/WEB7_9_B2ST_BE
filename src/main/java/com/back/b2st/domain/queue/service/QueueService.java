package com.back.b2st.domain.queue.service;

import com.back.b2st.domain.queue.dto.MoveResult;
import com.back.b2st.domain.queue.dto.QueueDefaultPolicy;
import com.back.b2st.domain.queue.dto.QueueEntryStatusCount;
import com.back.b2st.domain.queue.dto.response.QueueEntryRes;
import com.back.b2st.domain.queue.dto.response.QueuePositionRes;
import com.back.b2st.domain.queue.dto.response.QueueStatisticsRes;
import com.back.b2st.domain.queue.dto.response.StartBookingRes;
import com.back.b2st.domain.queue.entity.Queue;
import com.back.b2st.domain.queue.entity.QueueEntry;
import com.back.b2st.domain.queue.entity.QueueEntryStatus;
import com.back.b2st.domain.queue.error.QueueErrorCode;
import com.back.b2st.domain.queue.repository.QueueEntryRepository;
import com.back.b2st.domain.queue.repository.QueueRedisRepository;
import com.back.b2st.domain.queue.repository.QueueRepository;
import com.back.b2st.global.error.exception.BusinessException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
@Transactional(readOnly = true)
public class QueueService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final QueueRepository queueRepository;
	private final QueueEntryRepository queueEntryRepository;
	private final QueueRedisRepository queueRedisRepository;
	private final QueueManagementService queueManagementService;
	private final ScheduleResolver scheduleResolver;

	private <T> T runRedis(String op, Long queueId, Long userId, Supplier<T> supplier) {
		try {
			return supplier.get();
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.warn("Redis {} 실패 - queueId: {}, userId: {}", op, queueId, userId, e);
			throw new BusinessException(QueueErrorCode.REDIS_OPERATION_FAILED);
		}
	}

	private void runRedisVoid(String op, Long queueId, Long userId, Runnable runnable) {
		runRedis(op, queueId, userId, () -> {
			runnable.run();
			return null;
		});
	}

	private LocalDateTime nowKst() {
		return LocalDateTime.now(KST);
	}

	/**
	 * 대기열 입장 (내부 메서드)
	 *
	 * @param queueId 대기열 ID
	 * @param performanceId 공연 ID
	 * @param scheduleId 회차 ID (프론트 UX용)
	 * @param userId 사용자 ID
	 * @return 대기열 입장 결과
	 */
	@Transactional
	public QueueEntryRes enterQueue(Long queueId, Long performanceId, Long scheduleId, Long userId) {
		validateQueue(queueId);
		validateNotDuplicated(queueId, userId);

		long timestamp = Instant.now().toEpochMilli();

		runRedisVoid("addToWaitingQueue", queueId, userId,
			() -> queueRedisRepository.addToWaitingQueue(queueId, userId, timestamp)
		);

		Long rank0 = runRedis("getMyRank0InWaiting", queueId, userId,
			() -> queueRedisRepository.getMyRank0InWaiting(queueId, userId)
		);

		if (rank0 == null) {
			log.error("Redis 정합성 이슈: 방금 추가했는데 rank가 null - queueId: {}, userId: {}", queueId, userId);
			try {
				queueRedisRepository.removeFromWaitingQueue(queueId, userId);
			} catch (Exception rollbackEx) {
				log.error("enterQueue 롤백 실패 - queueId: {}, userId: {}", queueId, userId, rollbackEx);
			}
			throw new BusinessException(QueueErrorCode.QUEUE_DATA_INCONSISTENT);
		}

		int aheadCount = rank0.intValue();
		int myRank = rank0.intValue() + 1;

		return QueueEntryRes.waiting(queueId, performanceId, scheduleId, userId, aheadCount, myRank);
	}

	/**
	 * 대기열 입장 (기존 호환성 유지용 - deprecated)
	 *
	 * @deprecated performanceId, scheduleId를 포함하는 enterQueue(Long, Long, Long, Long)를 사용하세요.
	 */
	@Deprecated
	@Transactional
	public QueueEntryRes enterQueue(Long queueId, Long userId) {
		Queue queue = validateQueue(queueId);
		// 성능상 이슈가 있을 수 있으나 호환성을 위해 유지
		log.warn("enterQueue(queueId, userId) is deprecated. Use enterQueue(queueId, performanceId, scheduleId, userId) instead.");
		return enterQueue(queueId, queue.getPerformanceId(), null, userId);
	}

	/**
	 * 예매 시작: scheduleId로 대기열 자동 생성 및 입장 (Idempotent)
	 *
	 * 공연 단위 큐를 보장: scheduleId → performanceId 변환 후 getOrCreate
	 *
	 * Idempotent 동작:
	 * - 이미 WAITING/ENTERABLE 상태면 409 대신 현재 상태 반환
	 * - 프론트는 응답의 status로 화면 렌더링 가능
	 * - 재시도/새로고침이 자주 일어나도 안전하게 처리
	 *
	 * @param scheduleId 공연 회차 ID (프론트 UX용 진입 정보)
	 * @param userId 사용자 ID
	 * @return 예매 시작 응답 (queueId, performanceId, scheduleId, entry 포함)
	 */
	@Transactional
	public StartBookingRes startBooking(Long scheduleId, Long userId) {
		// 1. scheduleId → performanceId 변환
		Long performanceId = scheduleResolver.resolvePerformanceId(scheduleId);
		log.debug("Resolved scheduleId: {} -> performanceId: {}", scheduleId, performanceId);

		// 2. 공연 단위 큐 조회 또는 생성 (멱등성 보장)
		Queue queue = queueManagementService.getOrCreateByPerformanceId(
			performanceId,
			QueueDefaultPolicy.defaultBooking()
		);

		Long queueId = queue.getId();
		log.info("Queue resolved/created - queueId: {}, performanceId: {}, scheduleId: {}",
			queueId, performanceId, scheduleId);

		// 3. 이미 WAITING 또는 ENTERABLE 상태인지 확인 (Idempotent)
		boolean inWaiting = runRedis("isInWaitingQueue", queueId, userId,
			() -> queueRedisRepository.isInWaitingQueue(queueId, userId)
		);

		boolean inEnterable = runRedis("isInEnterable", queueId, userId,
			() -> queueRedisRepository.isInEnterable(queueId, userId)
		);

		// 4. 이미 대기 중이거나 입장 가능한 상태면 현재 상태 반환
		if (inWaiting || inEnterable) {
			log.debug("User already in queue (idempotent) - queueId: {}, userId: {}, inWaiting: {}, inEnterable: {}",
				queueId, userId, inWaiting, inEnterable);

			QueuePositionRes position = getMyPosition(queueId, userId);
			QueueEntryRes entry = convertPositionToEntry(position, performanceId, scheduleId);

			return new StartBookingRes(
				queueId,
				performanceId,
				scheduleId,
				entry
			);
		}

		// 5. 새로운 입장 처리
		QueueEntryRes entry = enterQueue(queueId, performanceId, scheduleId, userId);

		// 6. StartBookingRes 반환
		return new StartBookingRes(
			queueId,
			performanceId,
			scheduleId,
			entry
		);
	}

	/**
	 * QueuePositionRes를 QueueEntryRes로 변환
	 */
	private QueueEntryRes convertPositionToEntry(QueuePositionRes position, Long performanceId, Long scheduleId) {
		return new QueueEntryRes(
			position.queueId(),
			performanceId,
			scheduleId,
			position.userId(),
			position.status(),
			position.aheadCount(),
			position.myRank()
		);
	}

	public QueuePositionRes getMyStatus(Long queueId, Long userId) {
		return getMyPosition(queueId, userId);
	}

	public QueuePositionRes getMyPosition(Long queueId, Long userId) {
		validateQueue(queueId);

		boolean inWaiting = runRedis("isInWaitingQueue", queueId, userId,
			() -> queueRedisRepository.isInWaitingQueue(queueId, userId)
		);

		if (inWaiting) {
			Long rank0 = runRedis("getMyRank0InWaiting", queueId, userId,
				() -> queueRedisRepository.getMyRank0InWaiting(queueId, userId)
			);

			if (rank0 == null) {
				log.error("Redis 정합성 이슈: WAITING 큐에 있는데 rank가 null - queueId: {}, userId: {}",
					queueId, userId);
				throw new BusinessException(QueueErrorCode.QUEUE_DATA_INCONSISTENT);
			}

			return QueuePositionRes.waiting(queueId, userId, rank0.intValue(), rank0.intValue() + 1);
		}

		boolean inEnterable = runRedis("isInEnterable", queueId, userId,
			() -> queueRedisRepository.isInEnterable(queueId, userId)
		);

		if (inEnterable) {
			return QueuePositionRes.enterable(queueId, userId);
		}

		Optional<QueueEntry> entryOpt = queueEntryRepository.findByQueueIdAndUserId(queueId, userId);
		if (entryOpt.isEmpty()) {
			return QueuePositionRes.notInQueue(queueId, userId);
		}

		return buildResponseByStatus(queueId, userId, entryOpt.get());
	}

	@Transactional
	public void moveToEnterable(Long queueId, Long userId) {
		Queue queue = validateQueue(queueId);

		MoveResult result = runRedis("moveToEnterable", queueId, userId,
			() -> queueRedisRepository.moveToEnterable(
				queueId,
				userId,
				queue.getEntryTtlMinutes(),
				queue.getMaxActiveUsers()
			)
		);

		if (result == MoveResult.REJECTED_FULL || result == MoveResult.SKIPPED) {
			return;
		}

		LocalDateTime now = nowKst();
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

			entry.updateToEnterable(UUID.randomUUID(), now, now, expiresAt);
			queueEntryRepository.save(entry);

		} catch (DataAccessException e) {
			log.error("DB 저장 실패, Redis 롤백 시도 - queueId: {}, userId: {}", queueId, userId, e);
			try {
				queueRedisRepository.rollbackToWaiting(queueId, userId);
			} catch (Exception rollbackException) {
				log.error("Redis 롤백 실패(치명) - queueId: {}, userId: {}", queueId, userId, rollbackException);
			}
			throw new BusinessException(QueueErrorCode.QUEUE_INTERNAL_ERROR);
		}
	}

	@Transactional
	public void completeEntry(Long queueId, Long userId) {
		validateQueue(queueId);

		boolean isEnterable = runRedis("isInEnterable", queueId, userId,
			() -> queueRedisRepository.isInEnterable(queueId, userId)
		);

		if (!isEnterable) {
			throw new BusinessException(QueueErrorCode.QUEUE_ENTRY_EXPIRED);
		}

		LocalDateTime now = nowKst();

		QueueEntry entry = queueEntryRepository.findByQueueIdAndUserId(queueId, userId)
			.orElseGet(() -> QueueEntry.builder()
				.queueId(queueId)
				.userId(userId)
				.joinedAt(now)
				.enterableAt(now)
				.expiresAt(now) // 의미 없음(최소값), 즉시 COMPLETED로 전이됨
				.build()
			);

		entry.updateToCompleted(now);

		try {
			queueEntryRepository.save(entry);
		} catch (DataAccessException e) {
			throw new BusinessException(QueueErrorCode.QUEUE_INTERNAL_ERROR);
		}

		try {
			queueRedisRepository.removeFromEnterable(queueId, userId);
		} catch (Exception e) {
			log.warn("Redis 제거 실패(비중요) - queueId: {}, userId: {}", queueId, userId, e);
		}
	}

	@Transactional
	public void exitQueue(Long queueId, Long userId) {
		validateQueue(queueId);

		boolean inWaiting = runRedis("isInWaitingQueue", queueId, userId,
			() -> queueRedisRepository.isInWaitingQueue(queueId, userId)
		);

		if (inWaiting) {
			runRedisVoid("removeFromWaitingQueue", queueId, userId,
				() -> queueRedisRepository.removeFromWaitingQueue(queueId, userId)
			);
			return;
		}

		boolean inEnterable = runRedis("isInEnterable", queueId, userId,
			() -> queueRedisRepository.isInEnterable(queueId, userId)
		);

		if (inEnterable) {
			runRedisVoid("removeFromEnterable", queueId, userId,
				() -> queueRedisRepository.removeFromEnterable(queueId, userId)
			);

			queueEntryRepository.findByQueueIdAndUserId(queueId, userId).ifPresent(entry -> {
				if (entry.getStatus() == QueueEntryStatus.ENTERABLE) {
					entry.updateToExpired();
					queueEntryRepository.save(entry);
				}
			});
		}
	}

	public QueueStatisticsRes getQueueStatisticsForAdmin(Long queueId) {
		Queue queue = validateQueue(queueId);

		Long totalWaiting = runRedis("getTotalWaitingCount", queueId, null,
			() -> queueRedisRepository.getTotalWaitingCount(queueId)
		);

		Long totalEnterable = runRedis("getTotalEnterableCount", queueId, null,
			() -> queueRedisRepository.getTotalEnterableCount(queueId)
		);

		List<QueueEntryStatusCount> statusCounts = queueEntryRepository.countByStatusGrouped(queueId);

		return QueueStatisticsRes.of(
			queueId,
			totalWaiting != null ? totalWaiting.intValue() : 0,
			totalEnterable != null ? totalEnterable.intValue() : 0,
			queue.getMaxActiveUsers(),
			statusCounts
		);
	}

	private QueuePositionRes buildResponseByStatus(Long queueId, Long userId, QueueEntry entry) {
		return switch (entry.getStatus()) {
			case ENTERABLE -> QueuePositionRes.expired(queueId, userId); // SoT=Redis
			case EXPIRED -> QueuePositionRes.expired(queueId, userId);
			case COMPLETED -> QueuePositionRes.completed(queueId, userId);
			default -> QueuePositionRes.notInQueue(queueId, userId);
		};
	}

	private Queue validateQueue(Long queueId) {
		return queueRepository.findById(queueId)
			.orElseThrow(() -> new BusinessException(QueueErrorCode.QUEUE_NOT_FOUND));
	}

	private void validateNotDuplicated(Long queueId, Long userId) {
		boolean duplicated = runRedis("validateNotDuplicated(redis)", queueId, userId,
			() -> queueRedisRepository.isInWaitingQueue(queueId, userId)
				|| queueRedisRepository.isInEnterable(queueId, userId)
		);

		if (duplicated) {
			throw new BusinessException(QueueErrorCode.ALREADY_IN_QUEUE);
		}

		//EXIT/COMPLETE 이후엔 "처음부터 재진입" 허용
		//DB의 COMPLETED/EXPIRED 기록은 히스토리로만 남기고 재진입을 막지 않는다.
		//Optional<QueueEntry> existingEntry = queueEntryRepository.findByQueueIdAndUserId(queueId, userId);
		//if (existingEntry.isPresent() && existingEntry.get().getStatus() == QueueEntryStatus.COMPLETED) {
		//	throw new BusinessException(QueueErrorCode.ALREADY_IN_QUEUE);
		//}
	}
}
