package com.back.b2st.domain.queue.repository;

import java.util.Arrays;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import com.back.b2st.domain.queue.dto.MoveResult;
import com.back.b2st.domain.queue.error.QueueErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

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
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
public class QueueRedisRepository {

	private final StringRedisTemplate stringRedisTemplate;

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

	public void addToWaitingQueue(Long queueId, Long userId, long timestampMillis) {
		String key = getWaitingKey(queueId);
		stringRedisTemplate.opsForZSet().add(key, userId.toString(), timestampMillis);
	}

	public void removeFromWaitingQueue(Long queueId, Long userId) {
		String key = getWaitingKey(queueId);
		stringRedisTemplate.opsForZSet().remove(key, userId.toString());
	}

	/** 0-based rank */
	public Long getMyRank0InWaiting(Long queueId, Long userId) {
		String key = getWaitingKey(queueId);
		return stringRedisTemplate.opsForZSet().rank(key, userId.toString());
	}

	/** 1-based rank (테스트/편의용) */
	public Long getMyRankInWaiting(Long queueId, Long userId) {
		Long rank0 = getMyRank0InWaiting(queueId, userId);
		return rank0 == null ? null : (rank0 + 1);
	}

	public Long getTotalWaitingCount(Long queueId) {
		String key = getWaitingKey(queueId);
		Long size = stringRedisTemplate.opsForZSet().size(key);
		return size != null ? size : 0L;
	}

	public Set<String> getTopWaitingUsers(Long queueId, int count) {
		if (count <= 0) return java.util.Collections.emptySet();
		String key = getWaitingKey(queueId);
		Set<String> users = stringRedisTemplate.opsForZSet().range(key, 0, count - 1);
		return users != null ? users : java.util.Collections.emptySet();
	}

	public Set<ZSetOperations.TypedTuple<String>> getAllWaitingUsersWithScore(Long queueId) {
		String key = getWaitingKey(queueId);
		Set<ZSetOperations.TypedTuple<String>> result = stringRedisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
		return result != null ? result : java.util.Collections.emptySet();
	}

	public boolean isInWaitingQueue(Long queueId, Long userId) {
		String key = getWaitingKey(queueId);
		Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
		return score != null;
	}

	/* ==================== ENTERABLE (SoT: ZSET) ==================== */

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

	public void removeFromEnterable(Long queueId, Long userId) {
		String enterableKey = getEnterableKey(queueId);
		try {
			stringRedisTemplate.opsForZSet().remove(enterableKey, userId.toString());
		} catch (Exception e) {
			log.error("Redis removeFromEnterable failed - queueId: {}, userId: {}", queueId, userId, e);
			throw new BusinessException(QueueErrorCode.REDIS_OPERATION_FAILED);
		}
	}

	/**
	 * ✅ SoT=Redis: ZSET score로 유효성 판정
	 */
	public boolean isInEnterable(Long queueId, Long userId) {
		String enterableKey = getEnterableKey(queueId);
		long nowSeconds = System.currentTimeMillis() / 1000;

		Double score = stringRedisTemplate.opsForZSet().score(enterableKey, userId.toString());
		if (score == null) return false;

		return score.longValue() >= nowSeconds;
	}

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

	public Set<String> getAllEnterableUsers(Long queueId) {
		String enterableKey = getEnterableKey(queueId);
		long nowSeconds = System.currentTimeMillis() / 1000;

		Set<String> users = stringRedisTemplate.opsForZSet().rangeByScore(
			enterableKey,
			(double) nowSeconds,
			Double.POSITIVE_INFINITY
		);

		return users != null ? users : java.util.Collections.emptySet();
	}

	/**
	 * ENTERABLE ZSET 만료 정리 (score < nowSeconds)
	 */
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

	/**
	 * (호환용 alias) 기존 코드가 cleanupExpiredFromEnterable를 호출할 수도 있어서 제공
	 */
	public Long cleanupExpiredFromEnterable(Long queueId) {
		return cleanupExpiredEnterable(queueId);
	}

	public Long getEnterableTtlSecondsApprox(Long queueId, Long userId) {
		String enterableKey = getEnterableKey(queueId);
		long nowSeconds = System.currentTimeMillis() / 1000;

		Double score = stringRedisTemplate.opsForZSet().score(enterableKey, userId.toString());
		if (score == null) return null;

		long ttl = score.longValue() - nowSeconds;
		return Math.max(ttl, 0L);
	}

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

	/* ==================== TEST ONLY ==================== */

	@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
		name = "queue.test.enabled",
		havingValue = "true",
		matchIfMissing = false
	)
	public void clearAll(Long queueId) {
		String waitingKey = getWaitingKey(queueId);
		String enterableKey = getEnterableKey(queueId);

		stringRedisTemplate.delete(waitingKey);
		stringRedisTemplate.delete(enterableKey);

		log.info("Cleared all queue data (TEST ONLY) - queueId: {}", queueId);
	}

	public boolean exists(Long queueId) {
		String key = getWaitingKey(queueId);
		return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
	}
}
