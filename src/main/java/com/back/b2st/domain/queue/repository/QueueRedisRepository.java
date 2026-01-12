package com.back.b2st.domain.queue.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import com.back.b2st.domain.queue.dto.MoveResult;
import com.back.b2st.domain.queue.entity.QueueEntry;
import com.back.b2st.domain.queue.entity.QueueEntryStatus;
import com.back.b2st.domain.queue.error.QueueErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis Repository (Queue)
 *
 * ✅ SoT = ENTERABLE ZSET (member=userId, score=expiresAtSeconds)
 * - 유효 판정: score >= nowSeconds
 * - 카운트: ZCOUNT(nowSeconds, +inf)
 * - 만료 정리: ZREMRANGEBYSCORE(-inf, nowSeconds-1)
 *
 * ✅ Redis Cluster HashTag 적용
 * - 모든 키에 {queueId} 포함하여 같은 슬롯에 배치
 *
 * ✅ Circuit Breaker 적용
 * - 모든 Redis 호출에 Circuit Breaker 적용
 * - Fallback 전략: 중요 메서드는 DB 조회, 나머지는 안전한 기본값
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
public class QueueRedisRepository {

	private final StringRedisTemplate stringRedisTemplate;
	private final QueueEntryRepository queueEntryRepository; // Circuit Breaker Fallback용

	/**
	 * Lua Script: WAITING -> ENTERABLE 원자적 이동 + 상한 제어
	 * return:
	 * - 1: MOVED
	 * - 0: SKIPPED (WAITING 없음)
	 * - 2: REJECTED_FULL (상한 초과)
	 */
	@Autowired
	private RedisScript<Long> moveToEnterableScript;

	@Value("${spring.application.name:b2st}")
	private String appName;

	@Value("${spring.profiles.active:local}")
	private String profile;

	private static final String WAITING_KEY_PATTERN = "%s:%s:queue:{%d}:waiting";
	private static final String ENTERABLE_KEY_PATTERN = "%s:%s:queue:{%d}:enterable";

	private String getWaitingKey(Long queueId) {
		return String.format(WAITING_KEY_PATTERN, appName, profile, queueId);
	}

	private String getEnterableKey(Long queueId) {
		return String.format(ENTERABLE_KEY_PATTERN, appName, profile, queueId);
	}

	/* ==================== WAITING ==================== */

	@CircuitBreaker(name = "queueRedis", fallbackMethod = "addToWaitingQueueFallback")
	public void addToWaitingQueue(Long queueId, Long userId, long timestampMillis) {
		String key = getWaitingKey(queueId);
		stringRedisTemplate.opsForZSet().add(key, userId.toString(), timestampMillis);
	}

	private void addToWaitingQueueFallback(Long queueId, Long userId, long timestampMillis, Exception e) {
		log.error("Circuit Breaker activated - addToWaitingQueue failed for queueId: {}, userId: {}",
			queueId, userId, e);
		throw new BusinessException(QueueErrorCode.QUEUE_SERVICE_UNAVAILABLE,
			"대기열 시스템이 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요.");
	}

	@CircuitBreaker(name = "queueRedis", fallbackMethod = "removeFromWaitingQueueFallback")
	public void removeFromWaitingQueue(Long queueId, Long userId) {
		String key = getWaitingKey(queueId);
		stringRedisTemplate.opsForZSet().remove(key, userId.toString());
	}

	private void removeFromWaitingQueueFallback(Long queueId, Long userId, Exception e) {
		log.warn("Circuit Breaker activated - removeFromWaitingQueue failed for queueId: {}, userId: {}",
			queueId, userId, e);
		// WAITING 제거는 실패해도 크리티컬하지 않음 (스케줄러가 재정리)
	}

	/** 0-based rank */
	@CircuitBreaker(name = "queueRedis", fallbackMethod = "getMyRank0InWaitingFallback")
	public Long getMyRank0InWaiting(Long queueId, Long userId) {
		String key = getWaitingKey(queueId);
		return stringRedisTemplate.opsForZSet().rank(key, userId.toString());
	}

	private Long getMyRank0InWaitingFallback(Long queueId, Long userId, Exception e) {
		log.warn("Circuit Breaker activated - getMyRank0InWaiting fallback for queueId: {}, userId: {}",
			queueId, userId, e);
		return null; // 순번을 알 수 없음
	}

	/** 1-based rank (테스트/편의용) */
	@CircuitBreaker(name = "queueRedis", fallbackMethod = "getMyRankInWaitingFallback")
	public Long getMyRankInWaiting(Long queueId, Long userId) {
		Long rank0 = getMyRank0InWaiting(queueId, userId);
		return rank0 == null ? null : (rank0 + 1);
	}

	private Long getMyRankInWaitingFallback(Long queueId, Long userId, Exception e) {
		log.warn("Circuit Breaker activated - getMyRankInWaiting fallback for queueId: {}, userId: {}",
			queueId, userId, e);
		return null;
	}

	@CircuitBreaker(name = "queueRedis", fallbackMethod = "getTotalWaitingCountFallback")
	public Long getTotalWaitingCount(Long queueId) {
		String key = getWaitingKey(queueId);
		Long size = stringRedisTemplate.opsForZSet().size(key);
		return size != null ? size : 0L;
	}

	private Long getTotalWaitingCountFallback(Long queueId, Exception e) {
		log.warn("Circuit Breaker activated - getTotalWaitingCount fallback for queueId: {}", queueId, e);
		// WAITING은 DB에 저장 안하므로 알 수 없음
		return 0L;
	}

	@CircuitBreaker(name = "queueRedis", fallbackMethod = "getTopWaitingUsersFallback")
	public Set<String> getTopWaitingUsers(Long queueId, int count) {
		if (count <= 0) return Collections.emptySet();
		String key = getWaitingKey(queueId);
		Set<String> users = stringRedisTemplate.opsForZSet().range(key, 0, count - 1);
		return users != null ? users : Collections.emptySet();
	}

	private Set<String> getTopWaitingUsersFallback(Long queueId, int count, Exception e) {
		log.warn("Circuit Breaker activated - getTopWaitingUsers fallback for queueId: {}", queueId, e);
		return Collections.emptySet();
	}

	@CircuitBreaker(name = "queueRedis", fallbackMethod = "getAllWaitingUsersWithScoreFallback")
	public Set<ZSetOperations.TypedTuple<String>> getAllWaitingUsersWithScore(Long queueId) {
		String key = getWaitingKey(queueId);
		Set<ZSetOperations.TypedTuple<String>> result = stringRedisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
		return result != null ? result : Collections.emptySet();
	}

	private Set<ZSetOperations.TypedTuple<String>> getAllWaitingUsersWithScoreFallback(Long queueId, Exception e) {
		log.warn("Circuit Breaker activated - getAllWaitingUsersWithScore fallback for queueId: {}", queueId, e);
		return Collections.emptySet();
	}

	@CircuitBreaker(name = "queueRedis", fallbackMethod = "isInWaitingQueueFallback")
	public boolean isInWaitingQueue(Long queueId, Long userId) {
		String key = getWaitingKey(queueId);
		Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
		return score != null;
	}

	private boolean isInWaitingQueueFallback(Long queueId, Long userId, Exception e) {
		log.warn("Circuit Breaker activated - isInWaitingQueue fallback for queueId: {}, userId: {}",
			queueId, userId, e);
		// WAITING은 DB에 저장 안하므로 알 수 없음
		return false;
	}

	/* ==================== ENTERABLE (SoT: ZSET) ==================== */

	@CircuitBreaker(name = "queueRedis", fallbackMethod = "moveToEnterableFallback")
	public MoveResult moveToEnterable(Long queueId, Long userId, int ttlMinutes, int maxActiveUsers) {
		String waitingKey = getWaitingKey(queueId);
		String enterableKey = getEnterableKey(queueId);

		long nowSeconds = System.currentTimeMillis() / 1000;
		long expiresAtSeconds = nowSeconds + (ttlMinutes * 60L);

		final Long raw;
		try {
			raw = stringRedisTemplate.execute(
				moveToEnterableScript,
				Arrays.asList(waitingKey, enterableKey),
				userId.toString(),
				String.valueOf(expiresAtSeconds),
				String.valueOf(nowSeconds),
				String.valueOf(maxActiveUsers)
			);
		} catch (Exception e) {
			log.error("Redis Lua execute failed(moveToEnterable) - queueId: {}, userId: {}", queueId, userId, e);
			throw new BusinessException(QueueErrorCode.REDIS_OPERATION_FAILED);
		}

		if (raw == null) {
			log.error("Redis Lua result null(moveToEnterable) - queueId: {}, userId: {}", queueId, userId);
			throw new BusinessException(QueueErrorCode.REDIS_OPERATION_FAILED);
		}

		return switch (raw.intValue()) {
			case 1 -> MoveResult.MOVED;
			case 2 -> MoveResult.REJECTED_FULL;
			default -> MoveResult.SKIPPED;
		};
	}

	private MoveResult moveToEnterableFallback(Long queueId, Long userId, int ttlMinutes,
											   int maxActiveUsers, Exception e) {
		log.error("Circuit Breaker activated - moveToEnterable fallback for queueId: {}, userId: {}",
			queueId, userId, e);
		// 스케줄러가 다음 주기에 재시도하도록 SKIPPED 반환
		return MoveResult.SKIPPED;
	}

	@CircuitBreaker(name = "queueRedis", fallbackMethod = "removeFromEnterableFallback")
	public void removeFromEnterable(Long queueId, Long userId) {
		String enterableKey = getEnterableKey(queueId);
		try {
			stringRedisTemplate.opsForZSet().remove(enterableKey, userId.toString());
		} catch (Exception e) {
			log.error("Redis removeFromEnterable failed - queueId: {}, userId: {}", queueId, userId, e);
			throw new BusinessException(QueueErrorCode.REDIS_OPERATION_FAILED);
		}
	}

	private void removeFromEnterableFallback(Long queueId, Long userId, Exception e) {
		log.warn("Circuit Breaker activated - removeFromEnterable fallback for queueId: {}, userId: {}",
			queueId, userId, e);
		// Redis 제거 실패해도 DB에서 상태 변경은 Service에서 처리되므로 로그만
	}

	/**
	 * ✅ SoT=Redis: ZSET score로 유효성 판정
	 * 🔥 가장 중요한 메서드 - 예매 권한 검증에 사용
	 */
	@CircuitBreaker(name = "queueRedis", fallbackMethod = "isInEnterableFallback")
	public boolean isInEnterable(Long queueId, Long userId) {
		String enterableKey = getEnterableKey(queueId);
		long nowSeconds = System.currentTimeMillis() / 1000;

		Double score = stringRedisTemplate.opsForZSet().score(enterableKey, userId.toString());
		if (score == null) return false;

		return score.longValue() >= nowSeconds;
	}

	/**
	 * 🔥 CRITICAL: DB Fallback으로 권한 검증
	 * Redis 장애 시에도 예매가 가능하도록 DB에서 검증
	 */
	private boolean isInEnterableFallback(Long queueId, Long userId, Exception e) {
		log.error("Circuit Breaker activated - isInEnterable fallback (CRITICAL) for queueId: {}, userId: {}",
			queueId, userId, e);

		// DB에서 ENTERABLE 상태 확인 (중요!)
		boolean enterable = queueEntryRepository.existsByQueueIdAndUserIdAndStatus(
			queueId, userId, QueueEntryStatus.ENTERABLE);

		if (enterable) {
			log.warn("DB fallback success - user {} is ENTERABLE in queue {}", userId, queueId);
		} else {
			log.warn("DB fallback - user {} is NOT ENTERABLE in queue {}", userId, queueId);
		}

		return enterable;
	}

	@CircuitBreaker(name = "queueRedis", fallbackMethod = "getTotalEnterableCountFallback")
	public Long getTotalEnterableCount(Long queueId) {
		String enterableKey = getEnterableKey(queueId);
		long nowSeconds = System.currentTimeMillis() / 1000;

		Long count = stringRedisTemplate.opsForZSet().count(
			enterableKey,
			(double) nowSeconds,
			Double.POSITIVE_INFINITY
		);
		return count != null ? count : 0L;
	}

	private Long getTotalEnterableCountFallback(Long queueId, Exception e) {
		log.warn("Circuit Breaker activated - getTotalEnterableCount fallback for queueId: {}", queueId, e);

		// DB에서 ENTERABLE 상태 개수 조회
		long count = queueEntryRepository.countByQueueIdAndStatus(queueId, QueueEntryStatus.ENTERABLE);
		log.info("DB fallback - ENTERABLE count: {} for queueId: {}", count, queueId);
		return count;
	}

	@CircuitBreaker(name = "queueRedis", fallbackMethod = "getAllEnterableUsersFallback")
	public Set<String> getAllEnterableUsers(Long queueId) {
		String enterableKey = getEnterableKey(queueId);
		long nowSeconds = System.currentTimeMillis() / 1000;

		Set<String> users = stringRedisTemplate.opsForZSet().rangeByScore(
			enterableKey,
			(double) nowSeconds,
			Double.POSITIVE_INFINITY
		);

		return users != null ? users : Collections.emptySet();
	}

	private Set<String> getAllEnterableUsersFallback(Long queueId, Exception e) {
		log.warn("Circuit Breaker activated - getAllEnterableUsers fallback for queueId: {}", queueId, e);
		return Collections.emptySet();
	}

	/**
	 * ENTERABLE ZSET 만료 정리 (score < nowSeconds)
	 */
	@CircuitBreaker(name = "queueRedis", fallbackMethod = "cleanupExpiredEnterableFallback")
	public Long cleanupExpiredEnterable(Long queueId) {
		String enterableKey = getEnterableKey(queueId);
		long nowSeconds = System.currentTimeMillis() / 1000;

		Long removed = stringRedisTemplate.opsForZSet().removeRangeByScore(
			enterableKey,
			Double.NEGATIVE_INFINITY,
			(double) (nowSeconds - 1)
		);

		return removed != null ? removed : 0L;
	}

	private Long cleanupExpiredEnterableFallback(Long queueId, Exception e) {
		log.warn("Circuit Breaker activated - cleanupExpiredEnterable fallback for queueId: {}", queueId, e);
		// 스케줄러가 다음 주기에 재시도
		return 0L;
	}

	/**
	 * (호환용 alias) 기존 코드가 cleanupExpiredFromEnterable를 호출할 수도 있어서 제공
	 */
	@CircuitBreaker(name = "queueRedis", fallbackMethod = "cleanupExpiredFromEnterableFallback")
	public Long cleanupExpiredFromEnterable(Long queueId) {
		return cleanupExpiredEnterable(queueId);
	}

	private Long cleanupExpiredFromEnterableFallback(Long queueId, Exception e) {
		log.warn("Circuit Breaker activated - cleanupExpiredFromEnterable fallback for queueId: {}", queueId, e);
		return 0L;
	}

	@CircuitBreaker(name = "queueRedis", fallbackMethod = "getEnterableTtlSecondsApproxFallback")
	public Long getEnterableTtlSecondsApprox(Long queueId, Long userId) {
		String enterableKey = getEnterableKey(queueId);
		long nowSeconds = System.currentTimeMillis() / 1000;

		Double score = stringRedisTemplate.opsForZSet().score(enterableKey, userId.toString());
		if (score == null) return null;

		long ttl = score.longValue() - nowSeconds;
		return Math.max(ttl, 0L);
	}

	private Long getEnterableTtlSecondsApproxFallback(Long queueId, Long userId, Exception e) {
		log.warn("Circuit Breaker activated - getEnterableTtlSecondsApprox fallback for queueId: {}, userId: {}",
			queueId, userId, e);
		return null; // TTL 알 수 없음
	}

	@CircuitBreaker(name = "queueRedis", fallbackMethod = "rollbackToWaitingFallback")
	public void rollbackToWaiting(Long queueId, Long userId) {
		try {
			removeFromEnterable(queueId, userId);

			long timestamp = System.currentTimeMillis();
			addToWaitingQueue(queueId, userId, timestamp);

			log.info("Redis rollback ENTERABLE -> WAITING - queueId: {}, userId: {}", queueId, userId);
		} catch (Exception e) {
			log.error("Redis rollback failed - queueId: {}, userId: {}", queueId, userId, e);
			throw e;
		}
	}

	private void rollbackToWaitingFallback(Long queueId, Long userId, Exception e) {
		log.error("Circuit Breaker activated - rollbackToWaiting fallback for queueId: {}, userId: {}",
			queueId, userId, e);
		// Rollback 실패는 크리티컬 - 에러 전파
		throw new BusinessException(QueueErrorCode.QUEUE_SERVICE_UNAVAILABLE,
			"대기열 복구에 실패했습니다. 관리자에게 문의하세요.");
	}

	/* ==================== TEST ONLY ==================== */

	@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
		name = "queue.test.enabled",
		havingValue = "true",
		matchIfMissing = false
	)
	@CircuitBreaker(name = "queueRedis", fallbackMethod = "clearAllFallback")
	public void clearAll(Long queueId) {
		String waitingKey = getWaitingKey(queueId);
		String enterableKey = getEnterableKey(queueId);

		stringRedisTemplate.delete(waitingKey);
		stringRedisTemplate.delete(enterableKey);

		log.info("Cleared all queue data (TEST ONLY) - queueId: {}", queueId);
	}

	private void clearAllFallback(Long queueId, Exception e) {
		log.warn("Circuit Breaker activated - clearAll fallback (TEST ONLY) for queueId: {}", queueId, e);
	}

	@CircuitBreaker(name = "queueRedis", fallbackMethod = "existsFallback")
	public boolean exists(Long queueId) {
		String key = getWaitingKey(queueId);
		return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
	}

	private boolean existsFallback(Long queueId, Exception e) {
		log.warn("Circuit Breaker activated - exists fallback for queueId: {}", queueId, e);
		return false;
	}
}