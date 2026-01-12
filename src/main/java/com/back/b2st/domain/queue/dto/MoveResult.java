package com.back.b2st.domain.queue.dto;

/**
 * WAITING → ENTERABLE 이동 결과
 *
 * Lua 스크립트 반환값과 매핑:
 * - 1L → MOVED (성공)
 * - 0L → SKIPPED (이미 처리됨 또는 WAITING에 없음)
 * - -1L → REJECTED_FULL (maxActiveUsers 상한 초과로 거부)
 */
public enum MoveResult {
	/**
	 * WAITING → ENTERABLE 이동 성공
	 */
	MOVED,

	/**
	 * 이미 처리됨 또는 WAITING에 없음 (정상 스킵)
	 */
	SKIPPED,

	/**
	 * maxActiveUsers 상한 초과로 승격 거부 (정상, 다음 스케줄에 재시도)
	 */
	REJECTED_FULL
}

