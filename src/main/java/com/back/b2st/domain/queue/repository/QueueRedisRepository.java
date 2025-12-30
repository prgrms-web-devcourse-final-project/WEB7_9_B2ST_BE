package com.back.b2st.domain.queue.repository;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

/**
 * 대기열의 실시간 데이터를 Redis에 저장/조회/관리하는 Repository
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
public class QueueRedisRepository {

	private final RedisTemplate<String, Object> redisTemplate;

	// Lua Scripts
	@Autowired(required = false)
	private RedisScript<Long> moveToEnterableScript;

	@Autowired(required = false)
	private RedisScript<Long> removeFromEnterableScript;

	@Value("${spring.application.name:b2st}")
	private String appName;

	@Value("${spring.profiles.active:local}")
	private String profile;

	// Redis Key 패턴
	// enterableIndex:{queueId} (ZSET, score=expiresAt): 인덱스용 ZSET (만료 시간 기반 정리)
	// enterableToken:{queueId}:{userId} (STRING with TTL): 개별 토큰 (TTL로 자동 만료)
	private static final String WAITING_KEY_PATTERN = "%s:%s:queue:%d:waiting";                      // ZSET: 대기 중
	private static final String ENTERABLE_TOKEN_KEY_PATTERN = "%s:%s:queue:%d:enterable:token:%d";  // STRING: 개별 사용자 입장권 토큰 (TTL)
	private static final String ENTERABLE_INDEX_KEY_PATTERN = "%s:%s:queue:%d:enterable:index";     // ZSET: 전체 입장 가능자 인덱스 (score=expiresAt, 만료 시간 기반 정리)
	private static final String ENTERABLE_COUNT_KEY_PATTERN = "%s:%s:queue:%d:enterable:count";     // STRING: 누적 카운트

	@Deprecated
	private static final String ENTERABLE_USER_KEY_PATTERN = "%s:%s:queue:%d:enterable:%d";         // 레거시: enterableToken으로 대체 예정
	@Deprecated
	private static final String ENTERABLE_SET_KEY_PATTERN = "%s:%s:queue:%d:enterable";             // 레거시: enterableIndex로 대체 예정

	/**
	 * Redis Key 생성 헬퍼 메서드
	 */
	private String getWaitingKey(Long queueId) {
		return String.format(WAITING_KEY_PATTERN, appName, profile, queueId);
	}

	/**
	 * Enterable 토큰 키 생성
	 */
	private String getEnterableTokenKey(Long queueId, Long userId) {
		return String.format(ENTERABLE_TOKEN_KEY_PATTERN, appName, profile, queueId, userId);
	}

	/**
	 * Enterable 인덱스 키 생성
	 */
	private String getEnterableIndexKey(Long queueId) {
		return String.format(ENTERABLE_INDEX_KEY_PATTERN, appName, profile, queueId);
	}

	@Deprecated
	private String getEnterableUserKey(Long queueId, Long userId) {
		return getEnterableTokenKey(queueId, userId);
	}

	@Deprecated
	private String getEnterableSetKey(Long queueId) {
		return getEnterableIndexKey(queueId);
	}

	private String getEnterableCountKey(Long queueId) {
		return String.format(ENTERABLE_COUNT_KEY_PATTERN, appName, profile, queueId);
	}


	/* ==================== 대기열(WAITING) 관련 ==================== */

	/**
	 * 대기열에 사용자 추가
	 *
	 * @param queueId 대기열 ID
	 * @param userId 사용자 ID
	 * @param timestamp 입장 시각 (score로 사용, 먼저 들어온 순서대로 정렬)
	 */
	public void addToWaitingQueue(Long queueId, Long userId, long timestamp) {
		String key = getWaitingKey(queueId);
		redisTemplate.opsForZSet().add(key, userId.toString(), timestamp);

		log.debug("Added to waiting queue - queueId: {}, userId: {}, timestamp: {}", queueId, userId, timestamp);
	}

	/**
	 * 대기열에서 사용자 제거
	 */
	public void removeFromWaitingQueue(Long queueId, Long userId) {
		String key = getWaitingKey(queueId);
		redisTemplate.opsForZSet().remove(key, userId.toString());

		log.debug("Removed from waiting queue - queueId: {}, userId: {}", queueId, userId);
	}

	/**
	 * 내 현재 순번 조회 (1부터 시작)
	 *
	 * @return 1-based 순번 (대기열에 없으면 null)
	 */
	public Long getMyRankInWaiting(Long queueId, Long userId) {
		String key = getWaitingKey(queueId);
		Long rank = redisTemplate.opsForZSet().rank(key, userId.toString());

		return rank != null ? rank + 1 : null;
	}

	/**
	 * 나보다 앞에 대기 중인 사람 수
	 */
	public Long getWaitingAheadCount(Long queueId, Long userId) {
		Long rank = getMyRankInWaiting(queueId, userId);
		return rank != null ? rank - 1 : null;
	}

	/**
	 * 대기열 총 인원 수
	 */
	public Long getTotalWaitingCount(Long queueId) {
		String key = getWaitingKey(queueId);
		Long size = redisTemplate.opsForZSet().size(key);

		return size != null ? size : 0L;
	}

	/**
	 * 대기열 상위 N명 조회 (userId 목록)
	 */
	public Set<Object> getTopWaitingUsers(Long queueId, int count) {
		String key = getWaitingKey(queueId);
		return redisTemplate.opsForZSet().range(key, 0, count - 1);
	}

	/**
	 * 대기열 전체 조회 (userId + timestamp 포함)
	 */
	public Set<ZSetOperations.TypedTuple<Object>> getAllWaitingUsersWithScore(Long queueId) {
		String key = getWaitingKey(queueId);
		return redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
	}

	/**
	 * 대기열에 존재하는지 확인
	 */
	public boolean isInWaitingQueue(Long queueId, Long userId) {
		String key = getWaitingKey(queueId);
		Double score = redisTemplate.opsForZSet().score(key, userId.toString());

		return score != null;
	}


	/* ==================== 입장 가능(ENTERABLE) 관련 ==================== */

	/**
	 * WAITING → ENTERABLE 이동 (Lua Script - 원자적 실행)
	 *
	 * @param queueId 대기열 ID
	 * @param userId 사용자 ID
	 * @param ttlMinutes 입장권 유효 시간 (분) - Redis TTL로 자동 설정됨
	 */
	public void moveToEnterable(Long queueId, Long userId, int ttlMinutes) {
		// Lua Script 사용
		if (moveToEnterableScript != null) {
			String waitingKey = getWaitingKey(queueId);
			String tokenKey = getEnterableTokenKey(queueId, userId);
			String indexKey = getEnterableIndexKey(queueId);

			long expiresAtSeconds = System.currentTimeMillis() / 1000 + (ttlMinutes * 60);

			redisTemplate.execute(
				moveToEnterableScript,
				Arrays.asList(waitingKey, tokenKey, indexKey),
				userId.toString(),
				String.valueOf(ttlMinutes * 60), // 초 단위 TTL
				String.valueOf(expiresAtSeconds) // expiresAt timestamp (score)
			);

			log.info("Moved to enterable (Lua) - queueId: {}, userId: {}, ttl: {}min, expiresAt: {}",
				queueId, userId, ttlMinutes, expiresAtSeconds);
		} else {
			// Fallback: Lua Script 없을 때
			moveToEnterableFallback(queueId, userId, ttlMinutes);
		}
	}

	/**
	 * Fallback: Lua Script 없을 때 사용 (개발 환경)
	 */
	private void moveToEnterableFallback(Long queueId, Long userId, int ttlMinutes) {
		removeFromWaitingQueue(queueId, userId);

		String tokenKey = getEnterableTokenKey(queueId, userId);
		redisTemplate.opsForValue().set(tokenKey, "1", Duration.ofMinutes(ttlMinutes));

		String indexKey = getEnterableIndexKey(queueId);
		long expiresAtSeconds = System.currentTimeMillis() / 1000 + (ttlMinutes * 60);
		redisTemplate.opsForZSet().add(indexKey, userId.toString(), expiresAtSeconds);

		log.warn("Moved to enterable (Fallback) - queueId: {}, userId: {}, expiresAt: {}",
			queueId, userId, expiresAtSeconds);
	}

	/**
	 * ENTERABLE에서 사용자 제거 (Lua Script - 원자적 실행)
	 */
	public void removeFromEnterable(Long queueId, Long userId) {
		if (removeFromEnterableScript != null) {
			String tokenKey = getEnterableTokenKey(queueId, userId);
			String indexKey = getEnterableIndexKey(queueId);

			redisTemplate.execute(
				removeFromEnterableScript,
				Arrays.asList(tokenKey, indexKey),
				userId.toString()
			);

			log.debug("Removed from enterable (Lua) - queueId: {}, userId: {}", queueId, userId);
		} else {
			// Fallback
			removeFromEnterableFallback(queueId, userId);
		}
	}

	/**
	 * Fallback: Lua Script 없을 때 사용
	 */
	private void removeFromEnterableFallback(Long queueId, Long userId) {
		String tokenKey = getEnterableTokenKey(queueId, userId);
		redisTemplate.delete(tokenKey);

		String indexKey = getEnterableIndexKey(queueId);
		redisTemplate.opsForZSet().remove(indexKey, userId.toString());

		log.warn("Removed from enterable (Fallback) - queueId: {}, userId: {}", queueId, userId);
	}

	/**
	 * ENTERABLE 상태인지 확인
	 */
	public boolean isInEnterable(Long queueId, Long userId) {
		String tokenKey = getEnterableTokenKey(queueId, userId);
		return Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey));
	}

	/**
	 * ENTERABLE → WAITING 롤백 (DB 저장 실패 시 사용)
	 *
	 * @param queueId 대기열 ID
	 * @param userId 사용자 ID
	 */
	public void rollbackToWaiting(Long queueId, Long userId) {
		try {
			// 1. ENTERABLE에서 제거
			removeFromEnterable(queueId, userId);

			// 2. WAITING에 다시 추가 (현재 시간으로)
			long timestamp = System.currentTimeMillis();
			addToWaitingQueue(queueId, userId, timestamp);

			log.info("Redis 롤백 완료 (ENTERABLE → WAITING, 뒤로 보냄) - queueId: {}, userId: {}", queueId, userId);
		} catch (Exception e) {
			log.error("Redis 롤백 실패 - queueId: {}, userId: {}", queueId, userId, e);
			throw e;
		}
	}

	/**
	 * 현재 ENTERABLE 상태인 사람 수
	 *
	 * @param queueId 대기열 ID
	 * @return 유효한 ENTERABLE 사용자 수
	 */
	public Long getTotalEnterableCount(Long queueId) {
		String indexKey = getEnterableIndexKey(queueId);
		long nowSeconds = System.currentTimeMillis() / 1000;

		Long count = redisTemplate.opsForZSet().count(indexKey, nowSeconds, Double.POSITIVE_INFINITY);
		return count != null ? count : 0L;
	}

	/**
	 * ENTERABLE 사용자 전체 목록 조회 (ZSET 기반)
	 *
	 * @param queueId 대기열 ID
	 * @return 유효한 ENTERABLE 사용자 ID 목록
	 */
	public Set<Object> getAllEnterableUsers(Long queueId) {
		String indexKey = getEnterableIndexKey(queueId);
		long nowSeconds = System.currentTimeMillis() / 1000;

		Set<Object> validUsers = redisTemplate.opsForZSet().rangeByScore(
			indexKey,
			nowSeconds,
			Double.POSITIVE_INFINITY
		);

		return validUsers != null ? validUsers : java.util.Collections.emptySet();
	}

	/**
	 * 인덱스에서 만료된 사용자 제거 (ZSET 기반)
	 *
	 * @param queueId 대기열 ID
	 * @return 제거된 항목 수
	 */
	public Long cleanupExpiredFromIndex(Long queueId) {
		String indexKey = getEnterableIndexKey(queueId);
		long nowSeconds = System.currentTimeMillis() / 1000;

		Long removed = redisTemplate.opsForZSet().removeRangeByScore(
			indexKey,
			Double.NEGATIVE_INFINITY,
			nowSeconds - 1 // 현재 시간 이전 (1초 여유)
		);

		if (removed != null && removed > 0) {
			log.debug("ZSET 정리: 만료된 토큰 {}건 제거 - queueId: {}", removed, queueId);
		}

		return removed != null ? removed : 0L;
	}

	/**
	 * 입장권 남은 시간 조회 (초)
	 *
	 * @return 남은 시간(초), 없으면 null
	 */
	public Long getEnterableTtl(Long queueId, Long userId) {
		String tokenKey = getEnterableTokenKey(queueId, userId);
		return redisTemplate.getExpire(tokenKey);
	}


	/* ==================== 카운터 관련 ==================== */

	/**
	 * ENTERABLE 누적 카운트 증가 (지표/모니터링용)
	 *
	 * @return 증가된 후의 값
	 */
	public Long incrementEnterableCount(Long queueId) {
		String key = getEnterableCountKey(queueId);
		return redisTemplate.opsForValue().increment(key);
	}

	/**
	 * ENTERABLE 누적 카운트 조회 (지표/모니터링용)
	 */
	public Long getEnterableCount(Long queueId) {
		String key = getEnterableCountKey(queueId);
		Object count = redisTemplate.opsForValue().get(key);

		if (count == null) {
			return 0L;
		}

		// Number로 안전하게 변환
		if (count instanceof Number) {
			return ((Number) count).longValue();
		}

		// 예상치 못한 타입 발견 시 경고 로그
		log.warn("Unexpected count type: {} for queueId: {}", count.getClass().getSimpleName(), queueId);
		return 0L;
	}

	/**
	 * ENTERABLE 카운트 직접 설정 (테스트용)
	 */
	public void setEnterableCount(Long queueId, long count) {
		String key = getEnterableCountKey(queueId);
		redisTemplate.opsForValue().set(key, count);
	}


	/* ==================== 유틸리티 ==================== */

	/**
	 * 특정 대기열의 모든 Redis 데이터 삭제 (테스트 전용)
	 *
	 * @param queueId 대기열 ID
	 */
	@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
		name = "queue.test.enabled",
		havingValue = "true",
		matchIfMissing = false
	)
	public void clearAll(Long queueId) {
		String waitingKey = getWaitingKey(queueId);
		String enterableIndexKey = getEnterableIndexKey(queueId);
		String countKey = getEnterableCountKey(queueId);

		redisTemplate.delete(waitingKey);
		redisTemplate.delete(enterableIndexKey);
		redisTemplate.delete(countKey);

		String pattern = String.format("%s:%s:queue:%d:enterable:*", appName, profile, queueId);
		Set<String> keys = redisTemplate.keys(pattern);
		if (keys != null && !keys.isEmpty()) {
			redisTemplate.delete(keys);
		}

		log.info("Cleared all queue data (TEST ONLY) - queueId: {}", queueId);
	}

	/**
	 * 대기열이 존재하는지 확인
	 */
	public boolean exists(Long queueId) {
		String key = getWaitingKey(queueId);
		return Boolean.TRUE.equals(redisTemplate.hasKey(key));
	}
}

