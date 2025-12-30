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
 *
 * ⚠️ 분산 락 적용: 멀티 인스턴스 환경에서 중복 실행 방지
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
@Transactional(readOnly = true)
public class QueueSchedulerService {

	private final QueueRepository queueRepository;
	private final QueueRedisRepository queueRedisRepository;
	private final QueueService queueService;
	private final RedissonClient redissonClient;

	/**
	 * 대기열 자동 입장 처리 (분산 락 적용)
	 *
	 * 스케줄러에서 주기적으로 호출
	 * 1. 입장 가능 인원 계산
	 * 2. 상위 N명 입장 허용
	 *
	 * @param queueId 대기열 ID
	 * @param batchSize 한 번에 처리할 인원 (기본: 10명)
	 */
	@Transactional
	public void processNextEntries(Long queueId, int batchSize) {
		String lockKey = "queue:lock:process:" + queueId;
		RLock lock = redissonClient.getLock(lockKey);

		try {
			// 락 획득 시도 (최대 3초 대기, 10초 후 자동 해제)
			boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);

			if (!acquired) {
				log.warn("분산 락 획득 실패 (다른 서버에서 실행 중) - queueId: {}", queueId);
				return;
			}

			log.debug("분산 락 획득 성공 - queueId: {}", queueId);

			// 실제 처리 로직
			processEntriesInternal(queueId, batchSize);

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("분산 락 획득 중 인터럽트 발생 - queueId: {}", queueId, e);
		} catch (Exception e) {
			log.error("자동 입장 처리 중 오류 발생 - queueId: {}", queueId, e);
		} finally {
			// 락 해제 (획득한 스레드만 해제 가능)
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
				log.debug("분산 락 해제 - queueId: {}", queueId);
			}
		}
	}

	/**
	 * 실제 입장 처리 로직 (내부 메서드)
	 */
	private void processEntriesInternal(Long queueId, int batchSize) {
		// 1. Queue 조회
		Queue queue = queueRepository.findById(queueId)
			.orElseThrow(() -> new BusinessException(QueueErrorCode.QUEUE_NOT_FOUND));

		// 2. 대기 중인 인원 확인
		Long totalWaiting = getTotalWaitingWithFallback(queueId);
		if (totalWaiting == 0) {
			log.debug("대기 인원 없음 - queueId: {}", queueId);
			return;
		}

		// 3. 입장 가능 인원 확인
		Long currentEnterable = getEnterableCountWithFallback(queueId);
		int availableSlots = queue.getMaxActiveUsers() - currentEnterable.intValue();

		if (availableSlots <= 0) {
			log.debug("입장 가능 인원 없음 - queueId: {}, current: {}, max: {}",
				queueId, currentEnterable, queue.getMaxActiveUsers());
			return;
		}

		// 4. 실제 입장시킬 인원 계산
		int entryCount = Math.min(batchSize, Math.min(availableSlots, totalWaiting.intValue()));

		// 5. 상위 N명 추출
		Set<Object> topWaitingUsers = queueRedisRepository.getTopWaitingUsers(queueId, entryCount);
		if (topWaitingUsers.isEmpty()) {
			return;
		}

		List<Long> userIds = topWaitingUsers.stream()
			.map(obj -> Long.parseLong(obj.toString()))
			.collect(Collectors.toList());

		// 6. 입장 처리
		processBatchEntries(queueId, userIds);

		log.info("자동 입장 처리 완료 - queueId: {}, 처리 인원: {}명, 남은 대기: {}명",
			queueId, userIds.size(), getTotalWaitingWithFallback(queueId));
	}

	/**
	 * 배치 입장 처리
	 */
	@Transactional
	public void processBatchEntries(Long queueId, List<Long> userIds) {
		int successCount = 0;
		int failCount = 0;

		for (Long userId : userIds) {
			try {
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

			Long totalWaiting = getTotalWaitingWithFallback(queueId);
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
	 * 테스트용: 내 앞 사람들 모두 입장 처리 (분산 락 적용)
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
	 * Redis 대기 인원 조회 (Fallback)
	 */
	private Long getTotalWaitingWithFallback(Long queueId) {
		try {
			return queueRedisRepository.getTotalWaitingCount(queueId);
		} catch (Exception e) {
			log.warn("Redis 조회 실패, 0 반환 - queueId: {}", queueId, e);
			return 0L;
		}
	}

	/**
	 * Redis 입장 가능 인원 조회 (Fallback)
	 */
	private Long getEnterableCountWithFallback(Long queueId) {
		try {
			return queueRedisRepository.getEnterableCount(queueId);
		} catch (Exception e) {
			log.warn("Redis 조회 실패, 0 반환 - queueId: {}", queueId, e);
			return 0L;
		}
	}
}

