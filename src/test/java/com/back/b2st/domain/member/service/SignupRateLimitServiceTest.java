package com.back.b2st.domain.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import com.back.b2st.domain.member.error.MemberErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignupRateLimitService 단위 테스트")
class SignupRateLimitServiceTest {
	@Mock
	private StringRedisTemplate redisTemplate;
	@Mock
	private ValueOperations<String, String> valueOperations;
	@InjectMocks
	private SignupRateLimitService signupRateLimitService;
	private static final String TEST_IP = "192.168.1.100";

	@Nested
	@DisplayName("checkSignupLimit 메서드")
	class CheckSignupLimitTest {
		@Test
		@DisplayName("첫 번째 가입 시도 - 성공")
		void firstSignupAttempt_Success() {
			// given
			given(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
				.willReturn(1L);
			// when & then
			assertThatCode(() -> signupRateLimitService.checkSignupLimit(TEST_IP))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("세 번째 가입 시도 - 성공 (경계값)")
		void thirdSignupAttempt_Success() {
			// given
			given(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
				.willReturn(3L);
			// when & then
			assertThatCode(() -> signupRateLimitService.checkSignupLimit(TEST_IP))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("네 번째 가입 시도 - Rate Limit 초과 예외")
		void fourthSignupAttempt_ThrowsException() {
			// given
			given(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
				.willReturn(4L);
			// when & then
			assertThatThrownBy(() -> signupRateLimitService.checkSignupLimit(TEST_IP))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(MemberErrorCode.SIGNUP_RATE_LIMIT_EXCEEDED);
		}

		@Test
		@DisplayName("Redis 반환값 null - 기본값 1로 처리")
		void redisReturnsNull_TreatedAsOne() {
			// given
			given(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
				.willReturn(null);
			// when & then
			assertThatCode(() -> signupRateLimitService.checkSignupLimit(TEST_IP))
				.doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("getSignupAttemptCount 메서드")
	class GetSignupAttemptCountTest {
		@BeforeEach
		void setUp() {
			given(redisTemplate.opsForValue()).willReturn(valueOperations);
		}

		@Test
		@DisplayName("시도 횟수 조회 - 값 있음")
		void getCount_HasValue() {
			// given
			given(valueOperations.get(anyString())).willReturn("2");
			// when
			int count = signupRateLimitService.getSignupAttemptCount(TEST_IP);
			// then
			assertThat(count).isEqualTo(2);
		}

		@Test
		@DisplayName("시도 횟수 조회 - 값 없음 (0 반환)")
		void getCount_NoValue() {
			// given
			given(valueOperations.get(anyString())).willReturn(null);
			// when
			int count = signupRateLimitService.getSignupAttemptCount(TEST_IP);
			// then
			assertThat(count).isZero();
		}
	}

	@Nested
	@DisplayName("getRemainingSignupAttempts 메서드")
	class GetRemainingAttemptsTest {
		@BeforeEach
		void setUp() {
			given(redisTemplate.opsForValue()).willReturn(valueOperations);
		}

		@Test
		@DisplayName("남은 횟수 계산 - 2회 시도 시 1회 남음")
		void remaining_After2Attempts() {
			// given
			given(valueOperations.get(anyString())).willReturn("2");
			// when
			int remaining = signupRateLimitService.getRemainingSignupAttempts(TEST_IP);
			// then
			assertThat(remaining).isEqualTo(1);
		}

		@Test
		@DisplayName("남은 횟수 계산 - 초과 시 0 반환")
		void remaining_Exceeded() {
			// given
			given(valueOperations.get(anyString())).willReturn("5");
			// when
			int remaining = signupRateLimitService.getRemainingSignupAttempts(TEST_IP);
			// then
			assertThat(remaining).isZero();
		}
	}
}
