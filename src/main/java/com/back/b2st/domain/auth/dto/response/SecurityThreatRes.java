package com.back.b2st.domain.auth.dto.response;

import java.time.LocalDateTime;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * 보안 위협 DTO
 */
public record SecurityThreatRes(
	String clientIp,
	ThreatType threatType, // 위협 유형
	int count,
	SeverityLevel severity, // 심각도 수준
	LocalDateTime detectedAt // 탐지 시각
) {
	public enum ThreatType {
		CREDENTIAL_STUFFING, // 다수 계정 시도
		BRUTE_FORCE // 단일 계정 무차별 대입
	}

	public enum SeverityLevel {
		LOW, // 관찰 필요
		MEDIUM, // 주의
		HIGH, // 경고
		CRITICAL // 즉시 조치 필요
	}

	// Credential Stuffing 임계값 (10+계정:MEDIUM, 20+:HIGH, 50+:CRITICAL)
	private static final NavigableMap<Integer, SeverityLevel> STUFFING_THRESHOLDS = new TreeMap<>();
	// Brute Force 임계값 (50+실패:MEDIUM, 100+:HIGH, 200+:CRITICAL)
	private static final NavigableMap<Integer, SeverityLevel> BRUTE_FORCE_THRESHOLDS = new TreeMap<>();

	/**
	 * 임계값 초기화 블록
	 * - 클래스가 로딩될 때 한 번 실행되어 임계값 맵 설정
	 */
	static {
		STUFFING_THRESHOLDS.put(10, SeverityLevel.MEDIUM);
		STUFFING_THRESHOLDS.put(20, SeverityLevel.HIGH);
		STUFFING_THRESHOLDS.put(50, SeverityLevel.CRITICAL);

		BRUTE_FORCE_THRESHOLDS.put(50, SeverityLevel.MEDIUM);
		BRUTE_FORCE_THRESHOLDS.put(100, SeverityLevel.HIGH);
		BRUTE_FORCE_THRESHOLDS.put(200, SeverityLevel.CRITICAL);
	}

	/**
	 * 크리덴셜 스터핑 탐지 응답 생성
	 */
	public static SecurityThreatRes credentialStuffing(String ip, int distinctEmails) {
		return new SecurityThreatRes(
			ip,
			ThreatType.CREDENTIAL_STUFFING,
			distinctEmails,
			resolveSeverity(distinctEmails, STUFFING_THRESHOLDS),
			LocalDateTime.now()
		);
	}

	/**
	 * 브루트 포스 탐지 응답 생성
	 */
	public static SecurityThreatRes bruteForce(String ip, int failureCount) {
		return new SecurityThreatRes(
			ip,
			ThreatType.BRUTE_FORCE,
			failureCount,
			resolveSeverity(failureCount, BRUTE_FORCE_THRESHOLDS),
			LocalDateTime.now()
		);
	}

	/**
	 * 임계값 맵을 기반으로 심각도 수준 결정
	 */
	private static SeverityLevel resolveSeverity(int count, NavigableMap<Integer, SeverityLevel> thresholds) {
		// count 이하의 가장 큰 키에 해당하는 값 조회
		var entry = thresholds.floorEntry(count);
		// 해당 엔트리 없으면 LOW 반환
		return entry != null ? entry.getValue() : SeverityLevel.LOW;
	}
}
