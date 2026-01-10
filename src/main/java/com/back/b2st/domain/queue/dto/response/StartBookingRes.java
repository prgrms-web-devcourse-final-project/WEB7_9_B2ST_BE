package com.back.b2st.domain.queue.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 예매 시작 응답 DTO
 *
 * 프론트 UX가 "상세에서 회차 선택 → 대기열 진입"이므로 scheduleId를 입력으로 받고,
 * 서버 정책은 공연 단위 queueId 1개이므로 응답에 queueId, performanceId, scheduleId를 포함
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StartBookingRes(
	Long queueId,        // 공연 단위 큐 ID
	Long performanceId,  // 권한 체크/좌석 API의 기준
	Long scheduleId,     // 첫 진입 회차로 프론트가 세팅
	QueueEntryRes entry  // 대기열 입장 상세 정보
) {
}

