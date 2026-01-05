package com.back.b2st.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Redis Lua Script 설정
 *
 * ✅ WAITING: ZSET(score=timestampMillis)
 * ✅ ENTERABLE: ZSET(score=expiresAtSeconds) (SoT)
 */
@Configuration
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = false)
public class RedisScriptConfig {

	/**
	 * WAITING → ENTERABLE 이동 스크립트 (원자적 승격 + 상한 게이트)
	 *
	 * KEYS[1]: waitingKey (ZSET)
	 * KEYS[2]: enterableKey (ZSET, score=expiresAtSeconds)
	 *
	 * ARGV[1]: userId
	 * ARGV[2]: expiresAtSeconds
	 * ARGV[3]: nowSeconds
	 * ARGV[4]: maxActiveUsers
	 *
	 * Return:
	 *  1: MOVED
	 *  0: SKIPPED (WAITING에 없거나 이미 유효 ENTERABLE)
	 *  2: REJECTED_FULL
	 */
	@Bean
	public RedisScript<Long> moveToEnterableScript() {
		String script = """
			local userId = ARGV[1]
			local expiresAt = tonumber(ARGV[2])
			local now = tonumber(ARGV[3])
			local maxActive = tonumber(ARGV[4])

			-- 0) 이미 ENTERABLE이고 유효하면 idempotent skip
			local current = redis.call('ZSCORE', KEYS[2], userId)
			if current and tonumber(current) >= now then
				return 0
			end

			-- 1) WAITING에 있는지 확인 + 기존 score 확보(순번 보존)
			local waitingScore = redis.call('ZSCORE', KEYS[1], userId)
			if not waitingScore then
				return 0
			end

			-- 2) ENTERABLE 유효 인원 게이트
			local activeCount = redis.call('ZCOUNT', KEYS[2], now, '+inf')
			if activeCount >= maxActive then
				-- WAITING 유지(순번 유지) - ZREM 하지 않음
				return 2
			end

			-- 3) 이동 수행
			redis.call('ZREM', KEYS[1], userId)
			redis.call('ZADD', KEYS[2], expiresAt, userId)

			return 1
			""";

		return RedisScript.of(script, Long.class);
	}
}
