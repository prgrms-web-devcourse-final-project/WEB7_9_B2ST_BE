package com.back.b2st.domain.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.auth.dto.response.SecurityThreatRes;
import com.back.b2st.domain.auth.dto.response.SecurityThreatRes.ThreatType;
import com.back.b2st.domain.auth.repository.LoginLogRepository;

@ExtendWith(MockitoExtension.class)
class SecurityThreatDetectionServiceTest {

	private static final String TEST_IP = "192.168.1.100";
	private static final String TEST_IP_2 = "10.0.0.50";

	@InjectMocks
	private SecurityThreatDetectionService service;

	@Mock
	private LoginLogRepository loginLogRepository;

	@Nested
	@DisplayName("detectActiveThreats()")
	class DetectActiveThreatsTest {

		@Test
		@DisplayName("성공 - Credential Stuffing 위협 감지")
		void success_detectCredentialStuffing() {
			// given
			given(loginLogRepository.findDistinctClientIpsSince(any(LocalDateTime.class))).willReturn(List.of(TEST_IP));
			given(loginLogRepository.countDistinctEmailsByIpSince(eq(TEST_IP), any(LocalDateTime.class))).willReturn(
				15L); // 10 이상이면 Credential Stuffing

			// when
			List<SecurityThreatRes> result = service.detectActiveThreats();

			// then
			assertThat(result).hasSize(1);
			assertThat(result.get(0).threatType()).isEqualTo(ThreatType.CREDENTIAL_STUFFING);
			assertThat(result.get(0).clientIp()).isEqualTo(TEST_IP);
			assertThat(result.get(0).count()).isEqualTo(15);
		}

		@Test
		@DisplayName("성공 - Brute Force 위협 감지")
		void success_detectBruteForce() {
			// given
			given(loginLogRepository.findDistinctClientIpsSince(any(LocalDateTime.class))).willReturn(List.of(TEST_IP));
			given(loginLogRepository.countDistinctEmailsByIpSince(eq(TEST_IP), any(LocalDateTime.class))).willReturn(
				5L); // Credential Stuffing 임계값 미만
			given(loginLogRepository.countFailedAttemptsByIpSince(eq(TEST_IP), any(LocalDateTime.class))).willReturn(
				75L); // 50 이상이면 Brute Force

			// when
			List<SecurityThreatRes> result = service.detectActiveThreats();

			// then
			assertThat(result).hasSize(1);
			assertThat(result.get(0).threatType()).isEqualTo(ThreatType.BRUTE_FORCE);
			assertThat(result.get(0).count()).isEqualTo(75);
		}

		@Test
		@DisplayName("성공 - 여러 IP에서 각각 다른 위협 감지")
		void success_multipleIpsWithDifferentThreats() {
			// given
			given(loginLogRepository.findDistinctClientIpsSince(any(LocalDateTime.class))).willReturn(
				List.of(TEST_IP, TEST_IP_2));

			// IP 1: Credential Stuffing
			given(loginLogRepository.countDistinctEmailsByIpSince(eq(TEST_IP), any(LocalDateTime.class))).willReturn(
				25L);

			// IP 2: Brute Force
			given(loginLogRepository.countDistinctEmailsByIpSince(eq(TEST_IP_2), any(LocalDateTime.class))).willReturn(
				3L);
			given(loginLogRepository.countFailedAttemptsByIpSince(eq(TEST_IP_2), any(LocalDateTime.class))).willReturn(
				100L);

			// when
			List<SecurityThreatRes> result = service.detectActiveThreats();

			// then
			assertThat(result).hasSize(2);
			assertThat(result).extracting(SecurityThreatRes::threatType)
				.containsExactlyInAnyOrder(ThreatType.CREDENTIAL_STUFFING, ThreatType.BRUTE_FORCE);
		}

		@Test
		@DisplayName("성공 - 위협 없음 (임계값 미만)")
		void success_noThreatsDetected() {
			// given
			given(loginLogRepository.findDistinctClientIpsSince(any(LocalDateTime.class))).willReturn(List.of(TEST_IP));
			given(loginLogRepository.countDistinctEmailsByIpSince(eq(TEST_IP), any(LocalDateTime.class))).willReturn(
				3L); // Credential Stuffing 임계값 미만
			given(loginLogRepository.countFailedAttemptsByIpSince(eq(TEST_IP), any(LocalDateTime.class))).willReturn(
				20L); // Brute Force 임계값 미만

			// when
			List<SecurityThreatRes> result = service.detectActiveThreats();

			// then
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("성공 - 활성 IP 없음")
		void success_noActiveIps() {
			// given
			given(loginLogRepository.findDistinctClientIpsSince(any(LocalDateTime.class))).willReturn(List.of());

			// when
			List<SecurityThreatRes> result = service.detectActiveThreats();

			// then
			assertThat(result).isEmpty();
			then(loginLogRepository).should(never()).countDistinctEmailsByIpSince(any(), any());
			then(loginLogRepository).should(never()).countFailedAttemptsByIpSince(any(), any());
		}

		@Test
		@DisplayName("Credential Stuffing이 Brute Force보다 우선 (같은 IP)")
		void credentialStuffing_hasPriorityOverBruteForce() {
			// given - 둘 다 임계값 초과
			given(loginLogRepository.findDistinctClientIpsSince(any(LocalDateTime.class))).willReturn(List.of(TEST_IP));
			given(loginLogRepository.countDistinctEmailsByIpSince(eq(TEST_IP), any(LocalDateTime.class))).willReturn(
				30L); // Credential Stuffing 임계값 초과
			// Brute Force 체크는 호출되지 않아야 함

			// when
			List<SecurityThreatRes> result = service.detectActiveThreats();

			// then
			assertThat(result).hasSize(1);
			assertThat(result.get(0).threatType()).isEqualTo(ThreatType.CREDENTIAL_STUFFING);
			then(loginLogRepository).should(never()).countFailedAttemptsByIpSince(any(), any());
		}
	}

	@Nested
	@DisplayName("detectThreatForIp(String)")
	class DetectThreatForIpTest {

		@Test
		@DisplayName("성공 - Credential Stuffing 감지")
		void success_detectCredentialStuffing() {
			// given
			given(loginLogRepository.countDistinctEmailsByIpSince(eq(TEST_IP), any(LocalDateTime.class))).willReturn(
				12L);

			// when
			Optional<SecurityThreatRes> result = service.detectThreatForIp(TEST_IP);

			// then
			assertThat(result).isPresent();
			assertThat(result.get().threatType()).isEqualTo(ThreatType.CREDENTIAL_STUFFING);
		}

		@Test
		@DisplayName("성공 - Brute Force 감지")
		void success_detectBruteForce() {
			// given
			given(loginLogRepository.countDistinctEmailsByIpSince(eq(TEST_IP), any(LocalDateTime.class))).willReturn(
				5L);
			given(loginLogRepository.countFailedAttemptsByIpSince(eq(TEST_IP), any(LocalDateTime.class))).willReturn(
				60L);

			// when
			Optional<SecurityThreatRes> result = service.detectThreatForIp(TEST_IP);

			// then
			assertThat(result).isPresent();
			assertThat(result.get().threatType()).isEqualTo(ThreatType.BRUTE_FORCE);
		}

		@Test
		@DisplayName("성공 - 위협 없음")
		void success_noThreat() {
			// given
			given(loginLogRepository.countDistinctEmailsByIpSince(eq(TEST_IP), any(LocalDateTime.class))).willReturn(
				3L);
			given(loginLogRepository.countFailedAttemptsByIpSince(eq(TEST_IP), any(LocalDateTime.class))).willReturn(
				10L);

			// when
			Optional<SecurityThreatRes> result = service.detectThreatForIp(TEST_IP);

			// then
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("정확히 Credential Stuffing 임계값에서 감지")
		void exactlyAtCredentialStuffingThreshold() {
			// given
			given(loginLogRepository.countDistinctEmailsByIpSince(eq(TEST_IP), any(LocalDateTime.class))).willReturn(
				10L); // 임계값 정확히

			// when
			Optional<SecurityThreatRes> result = service.detectThreatForIp(TEST_IP);

			// then
			assertThat(result).isPresent();
			assertThat(result.get().threatType()).isEqualTo(ThreatType.CREDENTIAL_STUFFING);
		}

		@Test
		@DisplayName("정확히 Brute Force 임계값에서 감지")
		void exactlyAtBruteForceThreshold() {
			// given
			given(loginLogRepository.countDistinctEmailsByIpSince(eq(TEST_IP), any(LocalDateTime.class))).willReturn(
				5L);
			given(loginLogRepository.countFailedAttemptsByIpSince(eq(TEST_IP), any(LocalDateTime.class))).willReturn(
				50L); // 임계값 정확히

			// when
			Optional<SecurityThreatRes> result = service.detectThreatForIp(TEST_IP);

			// then
			assertThat(result).isPresent();
			assertThat(result.get().threatType()).isEqualTo(ThreatType.BRUTE_FORCE);
		}

		@Test
		@DisplayName("임계값 1 미만에서는 감지 안됨")
		void belowThresholdByOne() {
			// given
			given(loginLogRepository.countDistinctEmailsByIpSince(eq(TEST_IP), any(LocalDateTime.class))).willReturn(
				9L); // Credential Stuffing 임계값 - 1
			given(loginLogRepository.countFailedAttemptsByIpSince(eq(TEST_IP), any(LocalDateTime.class))).willReturn(
				49L); // Brute Force 임계값 - 1

			// when
			Optional<SecurityThreatRes> result = service.detectThreatForIp(TEST_IP);

			// then
			assertThat(result).isEmpty();
		}
	}
}
