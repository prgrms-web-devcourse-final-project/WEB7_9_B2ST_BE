package com.back.b2st.domain.queue.service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

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
	private final RedissonClient redissonClient;

	public void processNextEntries(Long queueId, int batchSize) {
		processEntriesInternal(queueId, batchSize);
	}

	private void processEntriesInternal(Long queueId, int batchSize) {
		Queue queue = queueRepository.findById(queueId)
			.orElseThrow(() -> new BusinessException(QueueErrorCode.QUEUE_NOT_FOUND));

		Long totalWaiting = getTotalWaiting(queueId);
		if (totalWaiting == null || totalWaiting == 0) return;

		Long currentEnterable = getEnterableCount(queueId);
		if (currentEnterable == null) return;

		int availableSlots = queue.getMaxActiveUsers() - currentEnterable.intValue();
		if (availableSlots <= 0) return;

		int entryCount = Math.min(batchSize, Math.min(availableSlots, totalWaiting.intValue()));

		Set<String> topWaitingUsers;
		try {
			topWaitingUsers = queueRedisRepository.getTopWaitingUsers(queueId, entryCount);
		} catch (Exception e) {
			log.error("Redis 상위 N명 추출 실패 - queueId: {}, entryCount: {}", queueId, entryCount, e);
			return;
		}

		if (topWaitingUsers == null || topWaitingUsers.isEmpty()) return;

		List<Long> userIds = topWaitingUsers.stream()
			.map(Long::parseLong)
			.collect(Collectors.toList());

		processBatchEntries(queueId, userIds);
		log.info("자동 입장 처리 완료 - queueId: {}, 처리 인원: {}명", queueId, userIds.size());
	}

	public void processBatchEntries(Long queueId, List<Long> userIds) {
		int success = 0;
		int fail = 0;

		for (Long userId : userIds) {
			try {
				queueService.moveToEnterable(queueId, userId);
				success++;
			} catch (Exception e) {
				fail++;
				log.error("입장 처리 실패 - queueId: {}, userId: {}, error: {}", queueId, userId, e.getMessage());
			}
		}
		log.info("배치 입장 처리 - queueId: {}, 성공: {}명, 실패: {}명", queueId, success, fail);
	}

	// ====== TEST UTIL (락 포함) ======

	public int processTopNForTest(Long queueId, int count) {
		String lockKey = "queue:lock:test:topN:" + queueId;
		RLock lock = redissonClient.getLock(lockKey);

		try {
			boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
			if (!acquired) return 0;

			Long totalWaiting = getTotalWaiting(queueId);
			if (totalWaiting == null || totalWaiting == 0) return 0;

			int actualCount = Math.min(count, totalWaiting.intValue());
			Set<String> topWaitingUsers = queueRedisRepository.getTopWaitingUsers(queueId, actualCount);
			if (topWaitingUsers == null || topWaitingUsers.isEmpty()) return 0;

			List<Long> userIds = topWaitingUsers.stream()
				.map(Long::parseLong)
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

	public int processUntilMeForTest(Long queueId, Long userId) {
		String lockKey = "queue:lock:test:untilMe:" + queueId + ":" + userId;
		RLock lock = redissonClient.getLock(lockKey);

		try {
			boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
			if (!acquired) return 0;

			Long myRank0 = queueRedisRepository.getMyRank0InWaiting(queueId, userId);
			if (myRank0 == null || myRank0 <= 0) return 0; // 0이면 이미 1등(앞사람 0명)

			int countToProcess = myRank0.intValue(); // 내 앞 사람 수 = rank0
			Set<String> topWaitingUsers = queueRedisRepository.getTopWaitingUsers(queueId, countToProcess);
			if (topWaitingUsers == null || topWaitingUsers.isEmpty()) return 0;

			List<Long> userIds = topWaitingUsers.stream()
				.map(Long::parseLong)
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

	public int processIncludingMeForTest(Long queueId, Long userId) {
		String lockKey = "queue:lock:test:includeMe:" + queueId + ":" + userId;
		RLock lock = redissonClient.getLock(lockKey);

		try {
			boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
			if (!acquired) return 0;

			Long myRank0 = queueRedisRepository.getMyRank0InWaiting(queueId, userId);
			if (myRank0 == null) return 0;

			int countToProcess = myRank0.intValue() + 1; // 나 포함
			Set<String> topWaitingUsers = queueRedisRepository.getTopWaitingUsers(queueId, countToProcess);
			if (topWaitingUsers == null || topWaitingUsers.isEmpty()) return 0;

			List<Long> userIds = topWaitingUsers.stream()
				.map(Long::parseLong)
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

	private Long getTotalWaiting(Long queueId) {
		try {
			Long count = queueRedisRepository.getTotalWaitingCount(queueId);
			return count != null ? count : 0L;
		} catch (Exception e) {
			log.error("Redis 대기 인원 조회 실패 - queueId: {}", queueId, e);
			return null;
		}
	}

	private Long getEnterableCount(Long queueId) {
		try {
			Long count = queueRedisRepository.getTotalEnterableCount(queueId);
			return count != null ? count : 0L;
		} catch (Exception e) {
			log.error("Redis 입장 가능 인원 조회 실패 - queueId: {}", queueId, e);
			return null;
		}
	}
}
