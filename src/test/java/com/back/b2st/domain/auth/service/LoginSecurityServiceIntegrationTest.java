package com.back.b2st.domain.auth.service;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.back.b2st.domain.auth.error.AuthErrorCode;
import com.back.b2st.global.error.exception.BusinessException;
import com.back.b2st.global.test.AbstractContainerBaseTest;

@SpringBootTest
@ActiveProfiles("test")
class LoginSecurityServiceIntegrationTest extends AbstractContainerBaseTest {
	private static final String TEST_EMAIL = "integration-test@example.com";
	private static final String TEST_IP = "192.168.100.1";
	@Autowired
	private LoginSecurityService loginSecurityService;
	@Autowired
	private StringRedisTemplate redisTemplate;

	@BeforeEach
	void setUp() {
		// 테스트 전 관련 키 정리
		cleanupTestKeys();
	}

	@AfterEach
	void tearDown() {
		// 테스트 후 정리
		cleanupTestKeys();
	}

	private void cleanupTestKeys() {
		redisTemplate.delete("login:attempt:" + TEST_EMAIL);
		redisTemplate.delete("login:lock:" + TEST_EMAIL);
	}

	@Nested
	@DisplayName("계정 잠금 통합 테스트")
	class AccountLockIntegrationTest {
		@Test
		@DisplayName("5회 연속 실패 시 계정 잠금")
		void whenFiveFailures_thenAccountLocked() {
			// given & when: 4회 실패 (잠금 전)
			for (int i = 0; i < 4; i++) {
				try {
					loginSecurityService.recordFailedAttempt(TEST_EMAIL, TEST_IP);
				} catch (BusinessException e) {
					fail("4회 이전에는 잠기면 안됨");
				}
			}
			// then: 5회째 실패 시 잠금 (보안상 LOGIN_FAILED로 반환)
			assertThatThrownBy(() -> loginSecurityService.recordFailedAttempt(TEST_EMAIL, TEST_IP))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.LOGIN_FAILED);
			// 잠금 상태 확인 (내부적으로는 잠금됨)
			assertThat(loginSecurityService.isAccountLocked(TEST_EMAIL)).isTrue();
		}

		@Test
		@DisplayName("잠긴 계정은 로그인 시도 차단")
		void whenLocked_thenLoginBlocked() {
			// given: 계정 잠금 상태 만들기
			for (int i = 0; i < 5; i++) {
				try {
					loginSecurityService.recordFailedAttempt(TEST_EMAIL, TEST_IP);
				} catch (BusinessException ignored) {
				}
			}
			// when & then: 잠금 확인 시 예외 (보안상 LOGIN_FAILED로 반환)
			assertThatThrownBy(() -> loginSecurityService.checkAccountLock(TEST_EMAIL))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.LOGIN_FAILED);
		}

		@Test
		@DisplayName("로그인 성공 시 실패 횟수 초기화")
		void whenLoginSuccess_thenFailureCountReset() {
			// given: 3회 실패
			for (int i = 0; i < 3; i++) {
				try {
					loginSecurityService.recordFailedAttempt(TEST_EMAIL, TEST_IP);
				} catch (BusinessException ignored) {
				}
			}
			assertThat(loginSecurityService.getFailedAttemptCount(TEST_EMAIL)).isEqualTo(3);
			// when: 로그인 성공
			loginSecurityService.onLoginSuccess(TEST_EMAIL, TEST_IP);
			// then: 실패 횟수 0으로 초기화
			assertThat(loginSecurityService.getFailedAttemptCount(TEST_EMAIL)).isZero();
		}

		@Test
		@DisplayName("남은 시도 횟수 정확히 계산")
		void remainingAttempts_calculatedCorrectly() {
			// given: 2회 실패
			for (int i = 0; i < 2; i++) {
				try {
					loginSecurityService.recordFailedAttempt(TEST_EMAIL, TEST_IP);
				} catch (BusinessException ignored) {
				}
			}
			// then: 5 - 2 = 3회 남음
			assertThat(loginSecurityService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(3);
		}
	}

	@Nested
	@DisplayName("Redis TTL 통합 테스트")
	class RedisTtlTest {
		@Test
		@DisplayName("시도 횟수 키에 TTL이 설정됨")
		void attemptKey_hasTtl() {
			// when: 첫 실패
			try {
				loginSecurityService.recordFailedAttempt(TEST_EMAIL, TEST_IP);
			} catch (BusinessException ignored) {
			}
			// then: TTL 확인 (10분 = 600초, 약간의 오차 허용)
			Long ttl = redisTemplate.getExpire("login:attempt:" + TEST_EMAIL, TimeUnit.SECONDS);
			assertThat(ttl).isNotNull();
			assertThat(ttl).isBetween(590L, 600L);
		}

		@Test
		@DisplayName("잠금 키에 TTL이 설정됨")
		void lockKey_hasTtl() {
			// given: 5회 실패로 잠금
			for (int i = 0; i < 5; i++) {
				try {
					loginSecurityService.recordFailedAttempt(TEST_EMAIL, TEST_IP);
				} catch (BusinessException ignored) {
				}
			}
			// then: TTL 확인 (10분 = 600초)
			Long ttl = redisTemplate.getExpire("login:lock:" + TEST_EMAIL, TimeUnit.SECONDS);
			assertThat(ttl).isNotNull();
			assertThat(ttl).isBetween(590L, 600L);
		}
	}
}
