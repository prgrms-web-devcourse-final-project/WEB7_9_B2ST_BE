package com.back.b2st.domain.member.listener;

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

import com.back.b2st.domain.member.dto.event.SignupEvent;
import com.back.b2st.domain.member.entity.SignupLog;
import com.back.b2st.domain.member.repository.SignupLogRepository;
import com.back.b2st.global.test.AbstractContainerBaseTest;

@SpringBootTest
@ActiveProfiles("test")
class SignupEventListenerIntegrationTest extends AbstractContainerBaseTest {

	private static final String TEST_EMAIL = "signup-event-test@example.com";
	private static final String TEST_IP = "10.0.0.100";

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	@Autowired
	private SignupLogRepository signupLogRepository;

	@AfterEach
	void tearDown() {
		// 테스트 데이터 정리
		List<SignupLog> logs = signupLogRepository.findTop10ByEmailOrderByCreatedAtDesc(TEST_EMAIL);
		signupLogRepository.deleteAll(logs);
	}

	@Test
	@DisplayName("가입 이벤트 발행 시 DB에 저장")
	void whenSignupEvent_thenSavedToDb() {
		// given
		SignupEvent event = SignupEvent.of(TEST_EMAIL, TEST_IP);

		// when
		eventPublisher.publishEvent(event);

		// then: 비동기이므로 대기 후 확인 (최대 5초)
		Awaitility.await()
			.atMost(5, TimeUnit.SECONDS)
			.untilAsserted(() -> {
				List<SignupLog> logs = signupLogRepository
					.findTop10ByEmailOrderByCreatedAtDesc(TEST_EMAIL);

				assertThat(logs).isNotEmpty();

				SignupLog saved = logs.get(0);
				assertThat(saved.getEmail()).isEqualTo(TEST_EMAIL);
				assertThat(saved.getClientIp()).isEqualTo(TEST_IP);
				assertThat(saved.getCreatedAt()).isNotNull();
			});
	}

	@Test
	@DisplayName("동일 IP에서 여러 가입 이벤트 발생 시 모두 저장")
	void whenMultipleSignupEvents_thenAllSaved() {
		// given
		String email1 = "multi-test-1@example.com";
		String email2 = "multi-test-2@example.com";
		String sameIp = "192.168.1.1";

		// when
		eventPublisher.publishEvent(SignupEvent.of(email1, sameIp));
		eventPublisher.publishEvent(SignupEvent.of(email2, sameIp));

		// then
		Awaitility.await()
			.atMost(5, TimeUnit.SECONDS)
			.untilAsserted(() -> {
				long count = signupLogRepository.countByClientIpSince(
					sameIp,
					java.time.LocalDateTime.now().minusMinutes(1));
				assertThat(count).isEqualTo(2);
			});

		// cleanup
		signupLogRepository.deleteAll(signupLogRepository.findTop10ByEmailOrderByCreatedAtDesc(email1));
		signupLogRepository.deleteAll(signupLogRepository.findTop10ByEmailOrderByCreatedAtDesc(email2));
	}
}
