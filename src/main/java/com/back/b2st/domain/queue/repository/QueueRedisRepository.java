package com.back.b2st.domain.queue.repository;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 대기열의 실시간 데이터를 Redis에 저장/조회/관리하는 Repository
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class QueueRedisRepository {

	private final RedisTemplate<String, Object> redisTemplate;

	// Lua Scripts (원자적 실행)
	@Autowired(required = false)
	private RedisScript<Long> moveToEnterableScript;

	@Autowired(required = false)
	private RedisScript<Long> removeFromEnterableScript;

	// 환경별 Key Prefix (대규모 트래픽 환경 - Namespace 분리)
	@Value("${spring.application.name:b2st}")
	private String appName;

	@Value("${spring.profiles.active:local}")
	private String profile;

	// Redis Key 패턴
	private static final String WAITING_KEY_PATTERN = "%s:%s:queue:%d:waiting";                      // ZSET: 대기 중
	private static final String ENTERABLE_USER_KEY_PATTERN = "%s:%s:queue:%d:enterable:%d";         // STRING:개별 사용자 입장권
	private static final String ENTERABLE_SET_KEY_PATTERN = "%s:%s:queue:%d:enterable";             // SET: 전체 입장 가능자 목록
	private static final String ENTERABLE_COUNT_KEY_PATTERN = "%s:%s:queue:%d:enterable:count";     // STRING: 누적 카운트

	/**
	 * Redis Key 생성 헬퍼 메서드
	 */
	private String getWaitingKey(Long queueId) {
		return String.format(WAITING_KEY_PATTERN, appName, profile, queueId);
	}

	private String getEnterableUserKey(Long queueId, Long userId) {
		return String.format(ENTERABLE_USER_KEY_PATTERN, appName, profile, queueId, userId);
	}

	private String getEnterableSetKey(Long queueId) {
		return String.format(ENTERABLE_SET_KEY_PATTERN, appName, profile, queueId);
	}

	private String getEnterableCountKey(Long queueId) {
		return String.format(ENTERABLE_COUNT_KEY_PATTERN, appName, profile, queueId);
	}

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

	/**
	 * WAITING → ENTERABLE 이동 (Lua Script - 원자적 실행)
	 *
	 * 대규모 트래픽 환경에서 원자성 보장:
	 * - 3개의 Redis 명령을 1번의 네트워크 호출로 실행
	 * - 중간 실패 시 자동 롤백
	 * - 성능 3배 향상 (RTT x3 → RTT x1)
	 *
	 * @param queueId 대기열 ID
	 * @param userId 사용자 ID
	 * @param ttlMinutes 입장권 유효 시간 (분)
	 */
	public void moveToEnterable(Long queueId, Long userId, int ttlMinutes) {
		// Lua Script 사용 (원자적 실행)
		if (moveToEnterableScript != null) {
			String waitingKey = getWaitingKey(queueId);
			String userKey = getEnterableUserKey(queueId, userId);
			String setKey = getEnterableSetKey(queueId);

			redisTemplate.execute(
				moveToEnterableScript,
				Arrays.asList(waitingKey, userKey, setKey),
				userId.toString(),
				String.valueOf(ttlMinutes * 60) // 초 단위로 변환
			);

			log.info("Moved to enterable (Lua) - queueId: {}, userId: {}, ttl: {}min", queueId, userId, ttlMinutes);
		} else {
			// Fallback: Lua Script 없을 때 (개발 환경)
			moveToEnterableFallback(queueId, userId, ttlMinutes);
		}
	}

	/**
	 * Fallback: Lua Script 없을 때 사용 (개발 환경)
	 */
	private void moveToEnterableFallback(Long queueId, Long userId, int ttlMinutes) {
		removeFromWaitingQueue(queueId, userId);

		String userKey = getEnterableUserKey(queueId, userId);
		redisTemplate.opsForValue().set(userKey, "1", Duration.ofMinutes(ttlMinutes));

		String setKey = getEnterableSetKey(queueId);
		redisTemplate.opsForSet().add(setKey, userId.toString());

		log.warn("Moved to enterable (Fallback) - queueId: {}, userId: {}", queueId, userId);
	}

	/**
	 * ENTERABLE에서 사용자 제거 (Lua Script)
	 */
	public void removeFromEnterable(Long queueId, Long userId) {
		// Lua Script 사용 (원자적 실행)
		if (removeFromEnterableScript != null) {
			String userKey = getEnterableUserKey(queueId, userId);
			String setKey = getEnterableSetKey(queueId);

			redisTemplate.execute(
				removeFromEnterableScript,
				Arrays.asList(userKey, setKey),
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
		String userKey = getEnterableUserKey(queueId, userId);
		redisTemplate.delete(userKey);

		String setKey = getEnterableSetKey(queueId);
		redisTemplate.opsForSet().remove(setKey, userId.toString());

		log.warn("Removed from enterable (Fallback) - queueId: {}, userId: {}", queueId, userId);
	}

	/**
	 * ENTERABLE 상태인지 확인
	 */
	public boolean isInEnterable(Long queueId, Long userId) {
		String userKey = getEnterableUserKey(queueId, userId);
		return Boolean.TRUE.equals(redisTemplate.hasKey(userKey));
	}

	/**
	 * 현재 ENTERABLE 상태인 사람 수
	 */
	public Long getTotalEnterableCount(Long queueId) {
		String setKey = getEnterableSetKey(queueId);
		Long size = redisTemplate.opsForSet().size(setKey);

		return size != null ? size : 0L;
	}

	/**
	 * ENTERABLE 사용자 전체 목록 조회
	 */
	public Set<Object> getAllEnterableUsers(Long queueId) {
		String setKey = getEnterableSetKey(queueId);
		return redisTemplate.opsForSet().members(setKey);
	}

	/**
	 * 입장권 남은 시간 조회 (초)
	 *
	 * @return 남은 시간(초), 없으면 null
	 */
	public Long getEnterableTtl(Long queueId, Long userId) {
		String userKey = getEnterableUserKey(queueId, userId);
		return redisTemplate.getExpire(userKey);
	}


	/* ==================== 카운터 관련 ==================== */

	/**
	 * ENTERABLE 누적 카운트 증가
	 *
	 * @return 증가된 후의 값
	 */
	public Long incrementEnterableCount(Long queueId) {
		String key = getEnterableCountKey(queueId);
		return redisTemplate.opsForValue().increment(key);
	}

	/**
	 * ENTERABLE 누적 카운트 조회
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

	/**
	 * 특정 대기열의 모든 Redis 데이터 삭제 (테스트/초기화용)
	 */
	public void clearAll(Long queueId) {
		String waitingKey = getWaitingKey(queueId);
		String enterableSetKey = getEnterableSetKey(queueId);
		String countKey = getEnterableCountKey(queueId);

		redisTemplate.delete(waitingKey);
		redisTemplate.delete(enterableSetKey);
		redisTemplate.delete(countKey);

		// ENTERABLE 개별 키들도 삭제 (패턴 매칭)
		String pattern = String.format("%s:%s:queue:%d:enterable:*", appName, profile, queueId);
		Set<String> keys = redisTemplate.keys(pattern);
		if (keys != null && !keys.isEmpty()) {
			redisTemplate.delete(keys);
		}

		log.info("Cleared all queue data - queueId: {}", queueId);
	}

	/**
	 * 대기열이 존재하는지 확인
	 */
	public boolean exists(Long queueId) {
		String key = getWaitingKey(queueId);
		return Boolean.TRUE.equals(redisTemplate.hasKey(key));
	}
}
