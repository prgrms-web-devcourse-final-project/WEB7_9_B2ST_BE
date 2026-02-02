package com.back.b2st.domain.performance.metrics;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class PerformanceMetrics {
	private final MeterRegistry registry;

	public PerformanceMetrics(MeterRegistry registry) {
		this.registry = registry;
	}

	/** 공연 상세 페이지 조회 기록 */
	public void recordView(Long performanceId) {
		Counter.builder("performance_view_total")
				.tag("performance_id", String.valueOf(performanceId))
				.register(registry)
				.increment();
	}

	/** 공연 회차 조회 기록 */
	public void recordScheduleView(Long scheduleId) {
		Counter.builder("schedule_view_total")
				.tag("schedule_id", String.valueOf(scheduleId))
				.register(registry)
				.increment();
	}

	/** 좌석 선택 기록 */
	public void recordSeatSelection(Long scheduleId, String seatGrade) {
		Counter.builder("seat_selection_total")
				.tag("schedule_id", String.valueOf(scheduleId))
				.tag("grade", seatGrade)
				.register(registry)
				.increment();
	}
}
