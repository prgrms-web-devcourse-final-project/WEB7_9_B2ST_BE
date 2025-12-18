package com.back.b2st.domain.email.service;

import static com.back.b2st.global.util.MaskingUtil.*;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.back.b2st.domain.email.error.EmailErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailRateLimiter {

	private final StringRedisTemplate redisTemplate;

	private static final String RATE_LIMIT_PREFIX = "email:rate:";
	private static final int MAX_REQUEST_PER_HOUR = 5;
	private static final long WINDOW_SECONDS = 3600; // 1시간

	public void checkRateLimit(String email) {
		String key = RATE_LIMIT_PREFIX + email;

		Long count = redisTemplate.opsForValue().increment(key);
		if (count == 1) {
			// 첫 요청일 때만 TTL 설정
			redisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
		}

		if (count > MAX_REQUEST_PER_HOUR) {
			log.warn("Rate Limit 초과: email={}, count={}", maskEmail(email), count);
			throw new BusinessException(EmailErrorCode.TOO_MANY_REQUESTS);
		}

		log.debug("Rate Limit 체크 통과: email={}, count={}/{}", maskEmail(email), count, MAX_REQUEST_PER_HOUR);
	}
}
