package com.back.b2st.domain.queue.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.queue.dto.MoveResult;
import com.back.b2st.domain.queue.dto.QueueEntryStatusCount;
import com.back.b2st.domain.queue.dto.response.QueueEntryRes;
import com.back.b2st.domain.queue.dto.response.QueuePositionRes;
import com.back.b2st.domain.queue.dto.response.QueueStatisticsRes;
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

	@Transactional
	public QueueEntryRes enterQueue(Long queueId, Long userId) {
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

		return QueueEntryRes.waiting(queueId, userId, aheadCount, myRank);
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

		Optional<QueueEntry> existingEntry = queueEntryRepository.findByQueueIdAndUserId(queueId, userId);
		if (existingEntry.isPresent() && existingEntry.get().getStatus() == QueueEntryStatus.COMPLETED) {
			throw new BusinessException(QueueErrorCode.ALREADY_IN_QUEUE);
		}
	}
}
