package com.back.b2st.domain.queue.service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.queue.entity.Queue;
import com.back.b2st.domain.queue.error.QueueErrorCode;
import com.back.b2st.domain.queue.repository.QueueRedisRepository;
import com.back.b2st.domain.queue.repository.QueueRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
public class QueueSchedulerService {

	private final QueueRepository queueRepository;
	private final QueueRedisRepository queueRedisRepository;
	private final QueueService queueService;

	// 테스트용 락 유지
	private final RedissonClient redissonClient;

	public void processNextEntries(Long queueId, int batchSize) {
		processEntriesInternal(queueId, batchSize);
	}

	private void processEntriesInternal(Long queueId, int batchSize) {
		Queue queue = queueRepository.findById(queueId)
			.orElseThrow(() -> new BusinessException(QueueErrorCode.QUEUE_NOT_FOUND));

		Long totalWaiting = safeTotalWaiting(queueId);
		if (totalWaiting == null || totalWaiting == 0) return;

		Long currentEnterable = safeEnterableCount(queueId);
		if (currentEnterable == null) return;

		int availableSlots = queue.getMaxActiveUsers() - currentEnterable.intValue();
		if (availableSlots <= 0) return;

		int entryCount = Math.min(batchSize, Math.min(availableSlots, totalWaiting.intValue()));
		if (entryCount <= 0) return;

		Set<Object> topWaitingUsers;
		try {
			topWaitingUsers = queueRedisRepository.getTopWaitingUsers(queueId, entryCount);
		} catch (Exception e) {
			log.error("Redis 상위 N명 추출 실패 - queueId: {}, entryCount: {}", queueId, entryCount, e);
			return;
		}

		if (topWaitingUsers == null || topWaitingUsers.isEmpty()) return;

		List<Long> userIds = topWaitingUsers.stream()
			.map(obj -> Long.parseLong(obj.toString()))
			.collect(Collectors.toList());

		processBatchEntries(queueId, userIds);
	}

	public void processBatchEntries(Long queueId, List<Long> userIds) {
		for (Long userId : userIds) {
			try {
				queueService.moveToEnterable(queueId, userId);
			} catch (Exception e) {
				log.error("입장 처리 실패 - queueId: {}, userId: {}", queueId, userId, e);
			}
		}
	}

	/* ==================== TEST ONLY ==================== */

	@Transactional
	public int processTopNForTest(Long queueId, int count) {
		String lockKey = "queue:lock:test:topN:" + queueId;
		RLock lock = redissonClient.getLock(lockKey);

		try {
			boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
			if (!acquired) return 0;

			Long totalWaiting = safeTotalWaiting(queueId);
			if (totalWaiting == null || totalWaiting == 0) return 0;

			int actualCount = Math.min(count, totalWaiting.intValue());
			Set<Object> topWaitingUsers = queueRedisRepository.getTopWaitingUsers(queueId, actualCount);
			if (topWaitingUsers == null || topWaitingUsers.isEmpty()) return 0;

			List<Long> userIds = topWaitingUsers.stream()
				.map(obj -> Long.parseLong(obj.toString()))
				.collect(Collectors.toList());

			processBatchEntries(queueId, userIds);
			return userIds.size();

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return 0;
		} finally {
			if (lock.isHeldByCurrentThread()) lock.unlock();
		}
	}

	@Transactional
	public int processUntilMeForTest(Long queueId, Long userId) {
		String lockKey = "queue:lock:test:untilMe:" + queueId + ":" + userId;
		RLock lock = redissonClient.getLock(lockKey);

		try {
			boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
			if (!acquired) return 0;

			Long myRank1 = queueRedisRepository.getMyRankInWaiting(queueId, userId);
			if (myRank1 == null || myRank1 <= 1) return 0;

			int countToProcess = myRank1.intValue() - 1;

			Set<Object> topWaitingUsers = queueRedisRepository.getTopWaitingUsers(queueId, countToProcess);
			if (topWaitingUsers == null || topWaitingUsers.isEmpty()) return 0;

			List<Long> userIds = topWaitingUsers.stream()
				.map(obj -> Long.parseLong(obj.toString()))
				.filter(id -> !id.equals(userId))
				.collect(Collectors.toList());

			if (userIds.isEmpty()) return 0;

			processBatchEntries(queueId, userIds);
			return userIds.size();

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return 0;
		} finally {
			if (lock.isHeldByCurrentThread()) lock.unlock();
		}
	}

	@Transactional
	public int processIncludingMeForTest(Long queueId, Long userId) {
		String lockKey = "queue:lock:test:includeMe:" + queueId + ":" + userId;
		RLock lock = redissonClient.getLock(lockKey);

		try {
			boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
			if (!acquired) return 0;

			Long myRank1 = queueRedisRepository.getMyRankInWaiting(queueId, userId);
			if (myRank1 == null) return 0;

			int countToProcess = myRank1.intValue();

			Set<Object> topWaitingUsers = queueRedisRepository.getTopWaitingUsers(queueId, countToProcess);
			if (topWaitingUsers == null || topWaitingUsers.isEmpty()) return 0;

			List<Long> userIds = topWaitingUsers.stream()
				.map(obj -> Long.parseLong(obj.toString()))
				.collect(Collectors.toList());

			processBatchEntries(queueId, userIds);
			return userIds.size();

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return 0;
		} finally {
			if (lock.isHeldByCurrentThread()) lock.unlock();
		}
	}

	private Long safeTotalWaiting(Long queueId) {
		try {
			return queueRedisRepository.getTotalWaitingCount(queueId);
		} catch (Exception e) {
			log.warn("Redis WAITING count 실패 - queueId={}", queueId, e);
			return null;
		}
	}

	private Long safeEnterableCount(Long queueId) {
		try {
			return queueRedisRepository.getTotalEnterableCount(queueId);
		} catch (Exception e) {
			log.warn("Redis ENTERABLE count 실패 - queueId={}", queueId, e);
			return null;
		}
	}
}
