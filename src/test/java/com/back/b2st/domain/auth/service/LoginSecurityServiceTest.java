package com.back.b2st.domain.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.back.b2st.domain.auth.error.AuthErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class LoginSecurityServiceTest {
	private static final String TEST_EMAIL = "test@example.com";
	private static final String TEST_IP = "192.168.1.1";
	@Mock
	private StringRedisTemplate redisTemplate;
	@Mock
	private ValueOperations<String, String> valueOperations;
	@InjectMocks
	private LoginSecurityService loginSecurityService;

	@Nested
	@DisplayName("checkAccountLock 테스트")
	class CheckAccountLockTest {
		@Test
		@DisplayName("잠기지 않은 계정은 예외 없이 통과")
		void whenNotLocked_thenNoException() {
			// given
			when(redisTemplate.hasKey("login:lock:" + TEST_EMAIL)).thenReturn(false);
			// when & then
			assertThatCode(() -> loginSecurityService.checkAccountLock(TEST_EMAIL))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("잠긴 계정은 LOGIN_FAILED 예외 발생 (보안: 잠금상태 숨김)")
		void whenLocked_thenThrowException() {
			// given
			when(redisTemplate.hasKey("login:lock:" + TEST_EMAIL)).thenReturn(true);
			when(redisTemplate.getExpire("login:lock:" + TEST_EMAIL, TimeUnit.SECONDS))
				.thenReturn(300L); // 5분 남음
			// when & then: 보안상 LOGIN_FAILED로 반환 (잠금 상태 숨김)
			assertThatThrownBy(() -> loginSecurityService.checkAccountLock(TEST_EMAIL))
				.isInstanceOf(BusinessException.class)
				.satisfies(e -> {
					BusinessException be = (BusinessException)e;
					assertThat(be.getErrorCode()).isEqualTo(AuthErrorCode.LOGIN_FAILED);
				});
		}
	}

	@Nested
	@DisplayName("recordFailedAttempt 테스트")
	class RecordFailedAttemptTest {
		@Test
		@DisplayName("5회 미만 실패 시 예외 없이 기록")
		void whenUnderMaxAttempts_thenJustRecord() {
			// given
			when(redisTemplate.execute(any(), anyList(), any()))
				.thenReturn(3L); // 3번째 시도
			// when & then
			assertThatCode(() -> loginSecurityService.recordFailedAttempt(TEST_EMAIL, TEST_IP))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("5회째 실패 시 계정 잠금 및 예외 발생")
		void whenReachMaxAttempts_thenLockAndThrow() {
			// given
			when(redisTemplate.opsForValue()).thenReturn(valueOperations);
			when(redisTemplate.execute(any(), anyList(), any()))
				.thenReturn(5L); // 5번째 시도
			// when & then
			assertThatThrownBy(() -> loginSecurityService.recordFailedAttempt(TEST_EMAIL, TEST_IP))
				.isInstanceOf(BusinessException.class)
				.satisfies(e -> {
					BusinessException be = (BusinessException)e;
					// 보안상 LOGIN_FAILED로 반환 (잠금 상태 숨김)
					assertThat(be.getErrorCode()).isEqualTo(AuthErrorCode.LOGIN_FAILED);
				});
			// 잠금 처리 확인
			verify(valueOperations).set(
				eq("login:lock:" + TEST_EMAIL),
				eq("locked"),
				eq(600L), // 10분
				eq(TimeUnit.SECONDS));
		}
	}

	@Nested
	@DisplayName("onLoginSuccess 테스트")
	class OnLoginSuccessTest {
		@Test
		@DisplayName("로그인 성공 시 시도 횟수 초기화")
		void whenSuccess_thenResetAttempts() {
			// when
			loginSecurityService.onLoginSuccess(TEST_EMAIL, TEST_IP);
			// then
			verify(redisTemplate).delete("login:attempt:" + TEST_EMAIL);
		}
	}

	@Nested
	@DisplayName("getFailedAttemptCount 테스트")
	class GetFailedAttemptCountTest {
		@Test
		@DisplayName("시도 기록이 없으면 0 반환")
		void whenNoAttempts_thenReturnZero() {
			// given
			when(redisTemplate.opsForValue()).thenReturn(valueOperations);
			when(valueOperations.get("login:attempt:" + TEST_EMAIL)).thenReturn(null);
			// when
			int count = loginSecurityService.getFailedAttemptCount(TEST_EMAIL);
			// then
			assertThat(count).isZero();
		}

		@Test
		@DisplayName("시도 기록이 있으면 해당 값 반환")
		void whenHasAttempts_thenReturnCount() {
			// given
			when(redisTemplate.opsForValue()).thenReturn(valueOperations);
			when(valueOperations.get("login:attempt:" + TEST_EMAIL)).thenReturn("3");
			// when
			int count = loginSecurityService.getFailedAttemptCount(TEST_EMAIL);
			// then
			assertThat(count).isEqualTo(3);
		}
	}
}
