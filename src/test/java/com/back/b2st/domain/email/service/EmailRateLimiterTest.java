package com.back.b2st.domain.email.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.back.b2st.domain.email.error.EmailErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class EmailRateLimiterTest {

	@InjectMocks
	private EmailRateLimiter rateLimiter;

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Test
	@DisplayName("첫 번째 요청 - TTL 설정과 함께 통과")
	void checkRateLimit_firstRequest() {
		// given
		String email = "test@example.com";
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.increment("email:rate:" + email)).willReturn(1L);

		// when
		rateLimiter.checkRateLimit(email);

		// then
		verify(redisTemplate).expire("email:rate:" + email, 3600, TimeUnit.SECONDS);
	}

	@Test
	@DisplayName("5번째 요청까지 통과")
	void checkRateLimit_withinLimit() {
		// given
		String email = "test@example.com";
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.increment("email:rate:" + email)).willReturn(5L);

		// when & then
		assertThatNoException().isThrownBy(() -> rateLimiter.checkRateLimit(email));
	}

	@Test
	@DisplayName("6번째 요청 - Rate Limit 초과 예외")
	void checkRateLimit_exceedsLimit() {
		// given
		String email = "test@example.com";
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.increment("email:rate:" + email)).willReturn(6L);

		// when & then
		assertThatThrownBy(() -> rateLimiter.checkRateLimit(email))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(EmailErrorCode.TOO_MANY_REQUESTS);
	}

	@Test
	@DisplayName("2~5번째 요청 - TTL 재설정 없이 통과 (count != 1)")
	void checkRateLimit_subsequentRequests() {
		// given
		String email = "test@example.com";
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.increment("email:rate:" + email)).willReturn(3L);

		// when
		rateLimiter.checkRateLimit(email);

		// then - expire가 호출되지 않음 (count != 1)
		verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
	}
}
