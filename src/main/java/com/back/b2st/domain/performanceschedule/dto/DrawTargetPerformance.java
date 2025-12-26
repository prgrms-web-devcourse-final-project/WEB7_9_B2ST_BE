package com.back.b2st.domain.performanceschedule.dto;

/**
 * 응모 내역 조회 대상 공연-회차
 * @param performanceId
 * @param performanceScheduleId
 */
public record DrawTargetPerformance(
	Long performanceId,
	Long performanceScheduleId
) {
}
