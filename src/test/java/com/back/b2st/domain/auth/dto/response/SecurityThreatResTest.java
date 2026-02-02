package com.back.b2st.domain.auth.dto.response;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.back.b2st.domain.auth.dto.response.SecurityThreatRes.SeverityLevel;
import com.back.b2st.domain.auth.dto.response.SecurityThreatRes.ThreatType;

class SecurityThreatResTest {

	private static final String TEST_IP = "192.168.1.100";

	@Nested
	@DisplayName("credentialStuffing() 팩토리 메서드")
	class CredentialStuffingTest {

		@ParameterizedTest(name = "계정 {0}개 시도 시 심각도는 {1}")
		@CsvSource({"5, LOW", "10, MEDIUM", "15, MEDIUM", "20, HIGH", "35, HIGH", "50, CRITICAL", "100, CRITICAL"})
		@DisplayName("계정 수에 따른 심각도 레벨 결정")
		void severityByDistinctEmails(int distinctEmails, SeverityLevel expected) {
			// when
			SecurityThreatRes result = SecurityThreatRes.credentialStuffing(TEST_IP, distinctEmails);

			// then
			assertThat(result.severity()).isEqualTo(expected);
			assertThat(result.threatType()).isEqualTo(ThreatType.CREDENTIAL_STUFFING);
			assertThat(result.clientIp()).isEqualTo(TEST_IP);
			assertThat(result.count()).isEqualTo(distinctEmails);
			assertThat(result.detectedAt()).isNotNull();
		}

		@Test
		@DisplayName("임계값 미만은 LOW 레벨")
		void belowThreshold_returnsLow() {
			// when
			SecurityThreatRes result = SecurityThreatRes.credentialStuffing(TEST_IP, 1);

			// then
			assertThat(result.severity()).isEqualTo(SeverityLevel.LOW);
		}

		@Test
		@DisplayName("정확히 임계값인 경우 해당 레벨 반환")
		void exactlyAtThreshold() {
			// when
			SecurityThreatRes medium = SecurityThreatRes.credentialStuffing(TEST_IP, 10);
			SecurityThreatRes high = SecurityThreatRes.credentialStuffing(TEST_IP, 20);
			SecurityThreatRes critical = SecurityThreatRes.credentialStuffing(TEST_IP, 50);

			// then
			assertThat(medium.severity()).isEqualTo(SeverityLevel.MEDIUM);
			assertThat(high.severity()).isEqualTo(SeverityLevel.HIGH);
			assertThat(critical.severity()).isEqualTo(SeverityLevel.CRITICAL);
		}
	}

	@Nested
	@DisplayName("bruteForce() 팩토리 메서드")
	class BruteForceTest {

		@ParameterizedTest(name = "실패 {0}회 시 심각도는 {1}")
		@CsvSource({"10, LOW", "49, LOW", "50, MEDIUM", "75, MEDIUM", "100, HIGH", "150, HIGH", "200, CRITICAL",
			"500, CRITICAL"})
		@DisplayName("실패 횟수에 따른 심각도 레벨 결정")
		void severityByFailureCount(int failures, SeverityLevel expected) {
			// when
			SecurityThreatRes result = SecurityThreatRes.bruteForce(TEST_IP, failures);

			// then
			assertThat(result.severity()).isEqualTo(expected);
			assertThat(result.threatType()).isEqualTo(ThreatType.BRUTE_FORCE);
			assertThat(result.clientIp()).isEqualTo(TEST_IP);
			assertThat(result.count()).isEqualTo(failures);
			assertThat(result.detectedAt()).isNotNull();
		}

		@Test
		@DisplayName("임계값 미만은 LOW 레벨")
		void belowThreshold_returnsLow() {
			// when
			SecurityThreatRes result = SecurityThreatRes.bruteForce(TEST_IP, 30);

			// then
			assertThat(result.severity()).isEqualTo(SeverityLevel.LOW);
		}

		@Test
		@DisplayName("정확히 임계값인 경우 해당 레벨 반환")
		void exactlyAtThreshold() {
			// when
			SecurityThreatRes medium = SecurityThreatRes.bruteForce(TEST_IP, 50);
			SecurityThreatRes high = SecurityThreatRes.bruteForce(TEST_IP, 100);
			SecurityThreatRes critical = SecurityThreatRes.bruteForce(TEST_IP, 200);

			// then
			assertThat(medium.severity()).isEqualTo(SeverityLevel.MEDIUM);
			assertThat(high.severity()).isEqualTo(SeverityLevel.HIGH);
			assertThat(critical.severity()).isEqualTo(SeverityLevel.CRITICAL);
		}
	}

	@Nested
	@DisplayName("Record 기본 동작")
	class RecordBehaviorTest {

		@Test
		@DisplayName("동일한 값으로 생성된 객체는 equals 성립")
		void equals_sameValues() {
			// given
			SecurityThreatRes res1 = SecurityThreatRes.credentialStuffing(TEST_IP, 10);
			SecurityThreatRes res2 = SecurityThreatRes.credentialStuffing(TEST_IP, 10);

			// then - detectedAt이 다르므로 equals가 false일 수 있음
			assertThat(res1.clientIp()).isEqualTo(res2.clientIp());
			assertThat(res1.threatType()).isEqualTo(res2.threatType());
			assertThat(res1.count()).isEqualTo(res2.count());
			assertThat(res1.severity()).isEqualTo(res2.severity());
		}

		@Test
		@DisplayName("toString은 모든 필드 포함")
		void toString_containsAllFields() {
			// given
			SecurityThreatRes res = SecurityThreatRes.bruteForce(TEST_IP, 100);

			// when
			String str = res.toString();

			// then
			assertThat(str).contains(TEST_IP);
			assertThat(str).contains("BRUTE_FORCE");
			assertThat(str).contains("100");
			assertThat(str).contains("HIGH");
		}
	}
}
