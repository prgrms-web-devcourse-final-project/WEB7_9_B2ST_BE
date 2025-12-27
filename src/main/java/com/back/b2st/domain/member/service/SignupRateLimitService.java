package com.back.b2st.domain.member.service;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.back.b2st.domain.member.error.MemberErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회원가입 Rate Limiting
 * - IP 기반 가입 횟수 제한
 * - 시간당 최대 3회
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SignupRateLimitService {

	// 세팅 상수
	private static final int MAX_SIGNUPS_PER_HOUR = 3; // 시간당 최대 가입 횟수
	private static final Duration WINDOW_DURATION = Duration.ofHours(1); // 윈도우 기간

	// Redis 키 접두사
	private static final String SIGNUP_KEY_PREFIX = "signup:ip:";

	// Lua 스크립트
	private static final String INCREMENT_SCRIPT = "local count = redis.call('INCR', KEYS[1]) " +
		"if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " + // 첫 시도면 만료시간 설정
		"return count";

	private final StringRedisTemplate redisTemplate;
	private final DefaultRedisScript<Long> incrementScript = createIncrementScript();

	private DefaultRedisScript<Long> createIncrementScript() {
		DefaultRedisScript<Long> script = new DefaultRedisScript<>();
		script.setScriptText(INCREMENT_SCRIPT);
		script.setResultType(Long.class);
		return script;
	}

	/**
	 * 가입 Rate Limit 검사
	 * - IP당 시간당 3회
	 *
	 * @param clientIp 클라이언트 IP 주소
	 */
	public void checkSignupLimit(String clientIp) {
		String key = SIGNUP_KEY_PREFIX + clientIp;
		Long count = redisTemplate.execute(
			incrementScript,
			List.of(key),
			String.valueOf(WINDOW_DURATION.getSeconds()));

		if (count == null) {
			count = 1L;
		}

		log.debug("가입 시도: IP={}, count={}/{}", clientIp, count, MAX_SIGNUPS_PER_HOUR);

		// 최대 횟수 초과 시 예외
		if (count > MAX_SIGNUPS_PER_HOUR) {
			log.warn("가입 시도 초과: IP={}, count={}", clientIp, count);
			throw new BusinessException(MemberErrorCode.SIGNUP_RATE_LIMIT_EXCEEDED);
		}
	}

	/**
	 * 현재 가입 시도 횟수 조회 (테스트/모니터링 용도)
	 *
	 * @param clientIp 클라이언트 IP
	 * @return 현재 시도 횟수 (없으면 0)
	 */
	public int getSignupAttemptCount(String clientIp) {
		String key = SIGNUP_KEY_PREFIX + clientIp;
		String value = redisTemplate.opsForValue().get(key);
		return value != null ? Integer.parseInt(value) : 0;
	}

	/**
	 * 남은 가입 횟수 조회
	 *
	 * @param clientIp 클라이언트 IP
	 * @return 남은 횟수
	 */
	public int getRemainingSignupAttempts(String clientIp) {
		return Math.max(0, MAX_SIGNUPS_PER_HOUR - getSignupAttemptCount(clientIp));
	}

}
