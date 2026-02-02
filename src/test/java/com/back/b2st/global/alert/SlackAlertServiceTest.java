package com.back.b2st.global.alert;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.back.b2st.domain.auth.dto.response.SecurityThreatRes;

class SlackAlertServiceTest {

	private static final String TEST_WEBHOOK_URL = "https://hooks.slack.com/services/T00/B00/XXX";
	private static final String TEST_IP = "192.168.1.100";

	@Nested
	@DisplayName("sendSecurityAlert() - 비활성화 상태")
	class SendSecurityAlertDisabledTest {

		@Test
		@DisplayName("enabled=false 시 예외 없이 종료")
		void disabled_doesNotThrow() {
			// given
			SlackAlertService service = new SlackAlertService(false, TEST_WEBHOOK_URL);
			SecurityThreatRes threat = SecurityThreatRes.credentialStuffing(TEST_IP, 15);

			// when & then
			assertThatCode(() -> service.sendSecurityAlert(threat)).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("URL 빈 문자열 시 예외 없이 종료")
		void emptyUrl_doesNotThrow() {
			// given
			SlackAlertService service = new SlackAlertService(true, "");
			SecurityThreatRes threat = SecurityThreatRes.bruteForce(TEST_IP, 100);

			// when & then
			assertThatCode(() -> service.sendSecurityAlert(threat)).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("URL이 공백만 있을 때 예외 없이 종료")
		void blankUrl_doesNotThrow() {
			// given
			SlackAlertService service = new SlackAlertService(true, "   ");
			SecurityThreatRes threat = SecurityThreatRes.credentialStuffing(TEST_IP, 20);

			// when & then
			assertThatCode(() -> service.sendSecurityAlert(threat)).doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("sendAccountLockedAlert() - 비활성화 상태")
	class SendAccountLockedAlertDisabledTest {

		@Test
		@DisplayName("enabled=false 시 예외 없이 종료")
		void disabled_doesNotThrow() {
			// given
			SlackAlertService service = new SlackAlertService(false, TEST_WEBHOOK_URL);

			// when & then
			assertThatCode(
				() -> service.sendAccountLockedAlert("test@example.com", TEST_IP)).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("URL 미설정 시 예외 없이 종료")
		void emptyUrl_doesNotThrow() {
			// given
			SlackAlertService service = new SlackAlertService(true, "");

			// when & then
			assertThatCode(
				() -> service.sendAccountLockedAlert("test@example.com", TEST_IP)).doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("API 호출 실패 시 예외 처리")
	class ApiFailureTest {

		@Test
		@DisplayName("잘못된 Webhook URL로 호출해도 예외 발생하지 않음")
		void invalidUrl_doesNotThrow() {
			// given
			SlackAlertService service = new SlackAlertService(true, "https://invalid.url/webhook");
			SecurityThreatRes threat = SecurityThreatRes.credentialStuffing(TEST_IP, 10);

			// when & then - 네트워크 오류가 발생해도 예외를 던지지 않음
			assertThatCode(() -> service.sendSecurityAlert(threat)).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("계정 잠금 알림도 오류 시 예외 발생하지 않음")
		void accountLocked_invalidUrl_doesNotThrow() {
			// given
			SlackAlertService service = new SlackAlertService(true, "https://invalid.url/webhook");

			// when & then
			assertThatCode(() -> service.sendAccountLockedAlert("user@test.com", TEST_IP)).doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("이메일 마스킹 검증 (maskEmail 메서드)")
	class EmailMaskingTest {

		@ParameterizedTest(name = "이메일={0}")
		@NullAndEmptySource
		@ValueSource(strings = {" ", "invalid", "noatsign", "@", "a@", "@b.com", "ab@test.com", "abcdef@example.com"})
		@DisplayName("다양한 이메일 형식에서 예외 발생 안함")
		void variousEmailFormats_noException(String email) {
			// given
			SlackAlertService service = new SlackAlertService(false, TEST_WEBHOOK_URL);

			// when & then
			assertThatCode(() -> service.sendAccountLockedAlert(email, TEST_IP)).doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("생성자 파라미터 테스트")
	class ConstructorTest {

		@ParameterizedTest(name = "enabled={0}, url={1}")
		@CsvSource({"true, https://hooks.slack.com/test", "false, https://hooks.slack.com/test", "true, ''",
			"false, ''"})
		@DisplayName("다양한 설정 조합으로 생성 가능")
		void variousConfigurations(boolean enabled, String webhookUrl) {
			// when & then
			assertThatCode(() -> new SlackAlertService(enabled, webhookUrl)).doesNotThrowAnyException();
		}
	}
}
