package com.back.b2st.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Redis Lua Script 설정
 *
 * 개발 초기 단계 - 대기열 기능 활성화 시에만 로드
 * application.yml에서 `queue.enabled: true` 설정 필요
 */
@Configuration
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
public class RedisScriptConfig {

	/**
	 * WAITING → ENTERABLE 이동 스크립트
	 *
	 * @return RedisScript<Long>
	 */
	@Bean
	public RedisScript<Long> moveToEnterableScript() {
		String script = """
			-- KEYS[1]: waiting key (ZSET, Hash Tag: {queueId})
			-- KEYS[2]: enterableToken:{queueId}:{userId} (STRING + TTL, Hash Tag: {queueId})
			-- KEYS[3]: enterableIndex:{queueId} (ZSET, score=expiresAt, Hash Tag: {queueId})
			-- ARGV[1]: userId (String)
			-- ARGV[2]: ttl (seconds, Integer)
			-- ARGV[3]: expiresAt (timestamp, seconds)
			
			--  ZREM 결과 체크: WAITING에 없으면 토큰 발급 안 함 (중복 발급/순서 무시 방지)
			local removed = redis.call('ZREM', KEYS[1], ARGV[1])
			if removed == 0 then
				return 0  -- 이미 처리됨 또는 WAITING에 없음
			end
			
			-- 2. 토큰 키 생성 + TTL 설정 (SETEX tokenKey ttl "1")
			redis.call('SETEX', KEYS[2], ARGV[2], '1')
			
			-- 3. 인덱스 ZSET에 추가 (ZADD indexKey expiresAt userId)
			redis.call('ZADD', KEYS[3], ARGV[3], ARGV[1])
			
			return 1  -- 성공
			""";

		return RedisScript.of(script, Long.class);
	}

	/**
	 * 대기열에 사용자 추가 + 카운트 증가 스크립트
	 *
	 * @return RedisScript<Long>
	 */
	@Bean
	public RedisScript<Long> addToWaitingWithCountScript() {
		String script = """
			-- KEYS[1]: waiting key (ZSET, Hash Tag: {queueId})
			-- KEYS[2]: count key (STRING, Hash Tag: {queueId})
			-- ARGV[1]: userId (String)
			-- ARGV[2]: timestamp (score)
			
			-- 클러스터 호환: KEYS[1]과 KEYS[2]는 같은 {queueId} Hash Tag를 포함해야 함
			-- 예: b2st:prod:queue:{1}:waiting, b2st:prod:queue:{1}:count
			
			-- 1. WAITING ZSET에 추가
			redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])
			
			-- 2. 카운트 증가
			local count = redis.call('INCR', KEYS[2])
			
			return count
			""";

		return RedisScript.of(script, Long.class);
	}

	/**
	 * ENTERABLE에서 제거 스크립트
	 *
	 * @return RedisScript<Long>
	 */
	@Bean
	public RedisScript<Long> removeFromEnterableScript() {
		String script = """
			-- KEYS[1]: enterableToken:{queueId}:{userId} (STRING)
			-- KEYS[2]: enterableIndex:{queueId} (ZSET)
			-- ARGV[1]: userId (String)
			
			-- 1. 토큰 키 삭제 (DEL tokenKey)
			redis.call('DEL', KEYS[1])
			
			-- 2. 인덱스 ZSET에서 제거 (ZREM indexKey userId)
			redis.call('ZREM', KEYS[2], ARGV[1])
			
			return 1
			""";

		return RedisScript.of(script, Long.class);
	}
}

