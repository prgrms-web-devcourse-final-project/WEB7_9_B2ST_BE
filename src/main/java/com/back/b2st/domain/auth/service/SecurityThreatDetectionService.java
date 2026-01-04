package com.back.b2st.domain.auth.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.auth.dto.response.SecurityThreatRes;
import com.back.b2st.domain.auth.repository.LoginLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 보안 위협 탐지 서비스
 * - 다수 계정 시도
 * - 단일 계정 무차별 대입
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SecurityThreatDetectionService {

	private final LoginLogRepository loginLogRepository;

	// 임계값 설정
	private static final int CREDITIAL_STUFFING_THRESHOLD = 10;
	private static final int BRUTE_FORCE_THRESHOLD = 50;

	/**
	 * 현재 활성 위협 목록 조회
	 */
	public List<SecurityThreatRes> detectActiveThreats() {
		LocalDateTime since = LocalDateTime.now().minusHours(1);

		// 최근 1시간 내 시도된 고유 IP 목록 조회
		return loginLogRepository.findDistinctClientIpsSince(since).stream()
			.map(ip -> detectThreatForIp(ip, since))
			.flatMap(Optional::stream)
			.peek(threat -> log.warn("[보안 위협] {} 감지: IP={}, 횟수={}",
				threat.threatType(), threat.clientIp(), threat.count()))
			.toList();
	}

	/**
	 * 특정 IP에 대한 위협 탐지 (public - 이벤트 트리거용)
	 */
	public Optional<SecurityThreatRes> detectThreatForIp(String clientIp) {
		return detectThreatForIp(clientIp, LocalDateTime.now().minusHours(1));
	}

	/**
	 * 특정 IP에 대한 위협 탐지 (private)
	 */
	private Optional<SecurityThreatRes> detectThreatForIp(String clientIp, LocalDateTime since) {

		// 다수 계정 시도 (Credential Stuffing) 탐지
		long distinctEmails = loginLogRepository.countDistinctEmailsByIpSince(clientIp, since);
		// 임계값 초과 시 위협으로 간주
		if (distinctEmails >= CREDITIAL_STUFFING_THRESHOLD) {
			return Optional.of(SecurityThreatRes.credentialStuffing(clientIp, (int)distinctEmails));
		}

		// 단일 계정 무차별 대입 (Brute Force) 탐지
		long failedAttempts = loginLogRepository.countFailedAttemptsByIpSince(clientIp, since);
		// 임계값 초과 시 위협으로 간주
		if (failedAttempts >= BRUTE_FORCE_THRESHOLD) {
			return Optional.of(SecurityThreatRes.bruteForce(clientIp, (int)failedAttempts));
		}

		return Optional.empty();
	}
}
