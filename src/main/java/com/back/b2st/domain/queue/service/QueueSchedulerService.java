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

/**
 * Queue 자동 처리 서비스 (스케줄러용)
 *
 * 자동 입장 처리, 만료 정리 등
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
public class QueueSchedulerService {

	private final QueueRepository queueRepository;
	private final QueueRedisRepository queueRedisRepository;
	private final QueueService queueService;
	private final RedissonClient redissonClient;

	/**
	 * 대기열 자동 입장 처리
	 *
	 * @param queueId 대기열 ID
	 * @param batchSize 한 번에 처리할 인원 (기본: 10명)
	 */
	public void processNextEntries(Long queueId, int batchSize) {
		// 리더 락은 QueueEntryScheduler에서 처리하므로
		// 여기서는 바로 처리 로직 실행
		processEntriesInternal(queueId, batchSize);
	}

	/**
	 * 실제 입장 처리 로직 (내부 메서드)
	 *
	 * - Redis 작업은 트랜잭션 밖에서 수행
	 * - 각 사용자별 처리는 moveToEnterable 내부의 짧은 트랜잭션에서 처리
	 */
	private void processEntriesInternal(Long queueId, int batchSize) {
		// 1. Queue 조회 (트랜잭션 없이 단건 읽기)
		Queue queue = queueRepository.findById(queueId)
			.orElseThrow(() -> new BusinessException(QueueErrorCode.QUEUE_NOT_FOUND));

		// 2. 대기 중인 인원 확인 (Redis, 트랜잭션 밖)
		Long totalWaiting = getTotalWaiting(queueId);
		if (totalWaiting == null) {
			log.error("Redis 대기 인원 조회 실패로 인한 중단 - queueId: {}", queueId);
			return;
		}
		if (totalWaiting == 0) {
			log.debug("대기 인원 없음 - queueId: {}", queueId);
			return;
		}

		// 3. 입장 가능 인원 확인 (Redis, 트랜잭션 밖)
		Long currentEnterable = getEnterableCount(queueId);
		if (currentEnterable == null) {
			log.error("Redis 조회 실패로 인한 중단 - queueId: {}", queueId);
			return;
		}

		int availableSlots = queue.getMaxActiveUsers() - currentEnterable.intValue();

		// 과대/과소평가 방지
		if (availableSlots < 0) {
			log.warn("availableSlots 음수 감지 (정합성 이슈 가능) - queueId: {}, current: {}, max: {}",
				queueId, currentEnterable, queue.getMaxActiveUsers());
			return;
		}

		if (availableSlots == 0) {
			log.debug("입장 가능 인원 없음 - queueId: {}, current: {}, max: {}",
				queueId, currentEnterable, queue.getMaxActiveUsers());
			return;
		}

		// 4. 실제 입장시킬 인원 계산
		int entryCount = Math.min(batchSize, Math.min(availableSlots, totalWaiting.intValue()));

		// 5. 상위 N명 추출 (Redis, 트랜잭션 밖)
		Set<Object> topWaitingUsers;
		try {
			topWaitingUsers = queueRedisRepository.getTopWaitingUsers(queueId, entryCount);
		} catch (Exception e) {
			log.error("Redis 상위 N명 추출 실패 - queueId: {}, entryCount: {}", queueId, entryCount, e);
			return;
		}

		if (topWaitingUsers.isEmpty()) {
			return;
		}

		List<Long> userIds = topWaitingUsers.stream()
			.map(obj -> Long.parseLong(obj.toString()))
			.collect(Collectors.toList());

		// 6. 입장 처리 (각 사용자별로 짧은 트랜잭션에서 처리)
		processBatchEntries(queueId, userIds);

		log.info("자동 입장 처리 완료 - queueId: {}, 처리 인원: {}명", queueId, userIds.size());
	}

	/**
	 * 배치 입장 처리
	 */
	public void processBatchEntries(Long queueId, List<Long> userIds) {
		int successCount = 0;
		int failCount = 0;

		for (Long userId : userIds) {
			try {
				// moveToEnterable 내부에서 @Transactional 처리됨
				queueService.moveToEnterable(queueId, userId);
				successCount++;
			} catch (Exception e) {
				failCount++;
				log.error("입장 처리 실패 - queueId: {}, userId: {}, error: {}",
					queueId, userId, e.getMessage());
			}
		}

		log.info("배치 입장 처리 - queueId: {}, 성공: {}명, 실패: {}명", queueId, successCount, failCount);
	}

	/**
	 * 테스트용: 상위 N명 입장 처리 (분산 락 적용)
	 */
	@Transactional
	public int processTopNForTest(Long queueId, int count) {
		String lockKey = "queue:lock:test:topN:" + queueId;
		RLock lock = redissonClient.getLock(lockKey);

		try {
			boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
			if (!acquired) {
				log.warn("[TEST] 분산 락 획득 실패 - queueId: {}", queueId);
				return 0;
			}

			Long totalWaiting = getTotalWaiting(queueId);
			if (totalWaiting == null) {
				log.error("[TEST] Redis 대기 인원 조회 실패 - queueId: {}", queueId);
				return 0;
			}
			if (totalWaiting == 0) {
				return 0;
			}

			int actualCount = Math.min(count, totalWaiting.intValue());

			Set<Object> topWaitingUsers = queueRedisRepository.getTopWaitingUsers(queueId, actualCount);
			if (topWaitingUsers.isEmpty()) {
				return 0;
			}

			List<Long> userIds = topWaitingUsers.stream()
				.map(obj -> Long.parseLong(obj.toString()))
				.collect(Collectors.toList());

			processBatchEntries(queueId, userIds);

			log.info("[TEST] 상위 {}명 입장 처리 완료 - queueId: {}", userIds.size(), queueId);
			return userIds.size();

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("[TEST] 분산 락 인터럽트 - queueId: {}", queueId, e);
			return 0;
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	/**
	 * 테스트용: 내 앞 사람들 모두 입장 처리
	 */
	@Transactional
	public int processUntilMeForTest(Long queueId, Long userId) {
		String lockKey = "queue:lock:test:untilMe:" + queueId + ":" + userId;
		RLock lock = redissonClient.getLock(lockKey);

		try {
			boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
			if (!acquired) {
				log.warn("[TEST] 분산 락 획득 실패 - queueId: {}, userId: {}", queueId, userId);
				return 0;
			}

			// 1. 내 순번 확인
			Long myRank = queueRedisRepository.getMyRankInWaiting(queueId, userId);
			if (myRank == null || myRank <= 1) {
				log.info("[TEST] 이미 1등이거나 대기열에 없음 - queueId: {}, userId: {}", queueId, userId);
				return 0;
			}

			// 2. 내 앞 사람들만 처리
			int countToProcess = myRank.intValue() - 1;

			Set<Object> topWaitingUsers = queueRedisRepository.getTopWaitingUsers(queueId, countToProcess);
			if (topWaitingUsers.isEmpty()) {
				return 0;
			}

			// 3. 나를 제외한 사람들만
			List<Long> userIds = topWaitingUsers.stream()
				.map(obj -> Long.parseLong(obj.toString()))
				.filter(id -> !id.equals(userId))
				.collect(Collectors.toList());

			if (userIds.isEmpty()) {
				return 0;
			}

			processBatchEntries(queueId, userIds);

			log.info("[TEST] 내 앞 사람 {}명 입장 처리 완료 - queueId: {}, userId: {}", userIds.size(), queueId, userId);
			return userIds.size();

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("[TEST] 분산 락 인터럽트 - queueId: {}, userId: {}", queueId, userId, e);
			return 0;
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	/**
	 * 테스트용: 나까지 포함해서 입장 처리 (분산 락 적용)
	 */
	@Transactional
	public int processIncludingMeForTest(Long queueId, Long userId) {
		String lockKey = "queue:lock:test:includeMe:" + queueId + ":" + userId;
		RLock lock = redissonClient.getLock(lockKey);

		try {
			boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
			if (!acquired) {
				log.warn("[TEST] 분산 락 획득 실패 - queueId: {}, userId: {}", queueId, userId);
				return 0;
			}

			// 1. 내 순번 확인
			Long myRank = queueRedisRepository.getMyRankInWaiting(queueId, userId);
			if (myRank == null) {
				log.warn("[TEST] 대기열에 없음 - queueId: {}, userId: {}", queueId, userId);
				return 0;
			}

			// 2. 나까지 포함해서 처리
			int countToProcess = myRank.intValue();

			Set<Object> topWaitingUsers = queueRedisRepository.getTopWaitingUsers(queueId, countToProcess);
			if (topWaitingUsers.isEmpty()) {
				return 0;
			}

			List<Long> userIds = topWaitingUsers.stream()
				.map(obj -> Long.parseLong(obj.toString()))
				.collect(Collectors.toList());

			processBatchEntries(queueId, userIds);

			log.info("[TEST] 나 포함 {}명 입장 처리 완료 - queueId: {}, userId: {}", userIds.size(), queueId, userId);
			return userIds.size();

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("[TEST] 분산 락 인터럽트 - queueId: {}, userId: {}", queueId, userId, e);
			return 0;
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	/**
	 * Redis 대기 인원 조회
	 */
	private Long getTotalWaiting(Long queueId) {
		try {
			Long count = queueRedisRepository.getTotalWaitingCount(queueId);
			return count != null ? count : 0L;
		} catch (Exception e) {
			log.error("Redis 대기 인원 조회 실패 - queueId: {}", queueId, e);
			// null 반환하여 상위에서 실패 처리
			return null;
		}
	}

	/**
	 * Redis 입장 가능 인원 조회 (현재 수)
	 */
	private Long getEnterableCount(Long queueId) {
		try {
			Long count = queueRedisRepository.getTotalEnterableCount(queueId);
			return count != null ? count : 0L;
		} catch (Exception e) {
			log.error("Redis 입장 가능 인원 조회 실패 - queueId: {}", queueId, e);
			// null 반환하여 상위에서 실패 처리
			return null;
		}
	}
}

