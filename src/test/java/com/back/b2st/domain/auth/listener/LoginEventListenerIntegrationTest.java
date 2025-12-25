package com.back.b2st.domain.auth.listener;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import com.back.b2st.domain.auth.dto.response.LoginEvent;
import com.back.b2st.domain.auth.entity.LoginLog;
import com.back.b2st.domain.auth.error.AuthErrorCode;
import com.back.b2st.domain.auth.repository.LoginLogRepository;
import com.back.b2st.domain.member.error.MemberErrorCode;
import com.back.b2st.global.test.AbstractContainerBaseTest;

/**
 * LoginEventListener 통합 테스트
 * - Testcontainers Redis 사용 (AbstractContainerBaseTest 상속)
 * - 이벤트 발행 → 비동기 처리 → DB 저장 검증
 */
@SpringBootTest
@ActiveProfiles("test")
class LoginEventListenerIntegrationTest extends AbstractContainerBaseTest {

	private static final String TEST_EMAIL = "event-test@example.com";
	private static final String TEST_IP = "10.0.0.1";
	@Autowired
	private ApplicationEventPublisher eventPublisher;
	@Autowired
	private LoginLogRepository loginLogRepository;

	@AfterEach
	void tearDown() {
		// 테스트 데이터 정리
		List<LoginLog> logs = loginLogRepository.findTop10ByEmailOrderByAttemptedAtDesc(TEST_EMAIL);
		loginLogRepository.deleteAll(logs);
	}

	@Test
	@DisplayName("로그인 성공 이벤트 발행 시 DB에 저장")
	void whenSuccessEvent_thenSavedToDb() {
		// given
		LoginEvent successEvent = LoginEvent.success(TEST_EMAIL, TEST_IP);

		// when
		eventPublisher.publishEvent(successEvent);

		// then: 비동기이므로 대기 후 확인 (최대 5초)
		Awaitility.await()
			.atMost(5, TimeUnit.SECONDS)
			.untilAsserted(() -> {
				List<LoginLog> logs = loginLogRepository
					.findTop10ByEmailOrderByAttemptedAtDesc(TEST_EMAIL);

				assertThat(logs).isNotEmpty();

				LoginLog saved = logs.get(0);
				assertThat(saved.getEmail()).isEqualTo(TEST_EMAIL);
				assertThat(saved.getClientIp()).isEqualTo(TEST_IP);
				assertThat(saved.isSuccess()).isTrue();
				assertThat(saved.getFailReason()).isNull();
			});
	}

	@Test
	@DisplayName("로그인 실패 이벤트 발행 시 INVALID_PASSWORD로 분류")
	void whenFailureEvent_thenSavedWithInvalidPassword() {
		// given: errorCode = null이면 INVALID_PASSWORD
		LoginEvent failEvent = LoginEvent.failure(
			TEST_EMAIL, TEST_IP, "비밀번호가 틀렸습니다", null);

		// when
		eventPublisher.publishEvent(failEvent);

		// then
		Awaitility.await()
			.atMost(5, TimeUnit.SECONDS)
			.untilAsserted(() -> {
				List<LoginLog> logs = loginLogRepository
					.findTop10ByEmailOrderByAttemptedAtDesc(TEST_EMAIL);

				assertThat(logs).isNotEmpty();

				LoginLog saved = logs.get(0);
				assertThat(saved.isSuccess()).isFalse();
				assertThat(saved.getFailReason())
					.isEqualTo(LoginLog.FailReason.INVALID_PASSWORD);
			});
	}

	@Test
	@DisplayName("계정 잠금 에러코드 전달 시 ACCOUNT_LOCKED로 분류")
	void whenLockedErrorCode_thenAccountLockedReason() {
		// given
		LoginEvent lockedEvent = LoginEvent.failure(
			TEST_EMAIL, TEST_IP,
			AuthErrorCode.ACCOUNT_LOCKED.getMessage(),
			AuthErrorCode.ACCOUNT_LOCKED);

		// when
		eventPublisher.publishEvent(lockedEvent);

		// then
		Awaitility.await()
			.atMost(5, TimeUnit.SECONDS)
			.untilAsserted(() -> {
				List<LoginLog> logs = loginLogRepository
					.findTop10ByEmailOrderByAttemptedAtDesc(TEST_EMAIL);

				assertThat(logs).isNotEmpty();
				assertThat(logs.get(0).getFailReason())
					.isEqualTo(LoginLog.FailReason.ACCOUNT_LOCKED);
			});
	}

	@Test
	@DisplayName("탈퇴 회원 에러코드 전달 시 ACCOUNT_WITHDRAWN으로 분류")
	void whenWithdrawnErrorCode_thenAccountWithdrawnReason() {
		// given
		LoginEvent withdrawnEvent = LoginEvent.failure(
			TEST_EMAIL, TEST_IP,
			MemberErrorCode.ALREADY_WITHDRAWN.getMessage(),
			MemberErrorCode.ALREADY_WITHDRAWN);

		// when
		eventPublisher.publishEvent(withdrawnEvent);

		// then
		Awaitility.await()
			.atMost(5, TimeUnit.SECONDS)
			.untilAsserted(() -> {
				List<LoginLog> logs = loginLogRepository
					.findTop10ByEmailOrderByAttemptedAtDesc(TEST_EMAIL);

				assertThat(logs).isNotEmpty();
				assertThat(logs.get(0).getFailReason())
					.isEqualTo(LoginLog.FailReason.ACCOUNT_WITHDRAWN);
			});
	}

	@Test
	@DisplayName("회원 미존재 에러코드 전달 시 ACCOUNT_NOT_FOUND로 분류")
	void whenMemberNotFoundErrorCode_thenAccountNotFoundReason() {
		// given
		LoginEvent notFoundEvent = LoginEvent.failure(
			TEST_EMAIL, TEST_IP,
			MemberErrorCode.MEMBER_NOT_FOUND.getMessage(),
			MemberErrorCode.MEMBER_NOT_FOUND);

		// when
		eventPublisher.publishEvent(notFoundEvent);

		// then
		Awaitility.await()
			.atMost(5, TimeUnit.SECONDS)
			.untilAsserted(() -> {
				List<LoginLog> logs = loginLogRepository
					.findTop10ByEmailOrderByAttemptedAtDesc(TEST_EMAIL);

				assertThat(logs).isNotEmpty();
				assertThat(logs.get(0).getFailReason())
					.isEqualTo(LoginLog.FailReason.ACCOUNT_NOT_FOUND);
			});
	}
}
