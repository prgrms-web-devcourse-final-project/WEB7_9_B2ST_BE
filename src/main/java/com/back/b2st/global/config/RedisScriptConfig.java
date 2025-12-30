package com.back.b2st.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Redis Lua Script 설정
 *
 * ⚠동시성 제어 방식: Lua 스크립트
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
			-- KEYS[1]: waiting key (ZSET)
			-- KEYS[2]: enterableToken:{queueId}:{userId} (STRING + TTL)
			-- KEYS[3]: enterableIndex:{queueId} (ZSET, score=expiresAt)
			-- ARGV[1]: userId (String)
			-- ARGV[2]: ttl (seconds, Integer)
			-- ARGV[3]: expiresAt (timestamp, seconds)
			
			-- 1. WAITING ZSET에서 제거
			redis.call('ZREM', KEYS[1], ARGV[1])
			
			-- 2. 토큰 키 생성 + TTL 설정 (SETEX tokenKey ttl "1")
			redis.call('SETEX', KEYS[2], ARGV[2], '1')
			
			-- 3. 인덱스 ZSET에 추가 (ZADD indexKey expiresAt userId)
			redis.call('ZADD', KEYS[3], ARGV[3], ARGV[1])
			
			return 1
			""";

		return RedisScript.of(script, Long.class);
	}

	/**
	 * 대기열에 사용자 추가 + 카운트 증가 스크립트
	 *
	 * 2개의 Redis 명령을 하나의 원자적 작업으로 실행:
	 * 1. WAITING ZSET에 추가
	 * 2. 전체 카운트 증가
	 *
	 * @return RedisScript<Long>
	 */
	@Bean
	public RedisScript<Long> addToWaitingWithCountScript() {
		String script = """
			-- KEYS[1]: waiting key (ZSET)
			-- KEYS[2]: count key (STRING)
			-- ARGV[1]: userId (String)
			-- ARGV[2]: timestamp (score)
			
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
	 * - KEYS[1]: enterableToken:{queueId}:{userId} (STRING)
	 * - KEYS[2]: enterableIndex:{queueId} (ZSET)
	 *
	 * 2개의 Redis 명령을 하나의 원자적 작업으로 실행:
	 * 1. 토큰 키 삭제 (DEL tokenKey)
	 * 2. 인덱스 ZSET에서 제거 (ZREM indexKey userId)
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

