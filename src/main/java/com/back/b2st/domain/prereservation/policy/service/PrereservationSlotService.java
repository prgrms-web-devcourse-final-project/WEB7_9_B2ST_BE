package com.back.b2st.domain.prereservation.policy.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.prereservation.entry.error.PrereservationErrorCode;
import com.back.b2st.domain.prereservation.policy.repository.PrereservationTimeTableRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrereservationSlotService {

	private final PrereservationTimeTableRepository prereservationTimeTableRepository;

	public Slot calculateSlotOrThrow(PerformanceSchedule schedule, Section section) {
		validateBookingTimeConfigured(schedule);

		var timeTable = prereservationTimeTableRepository
			.findTopByPerformanceScheduleIdAndSectionIdOrderByIdDesc(
				schedule.getPerformanceScheduleId(),
				section.getId()
			)
			.orElseThrow(() -> new BusinessException(PrereservationErrorCode.TIME_TABLE_NOT_CONFIGURED));

		return new Slot(timeTable.getBookingStartAt(), timeTable.getBookingEndAt());
	}

	private void validateBookingTimeConfigured(PerformanceSchedule schedule) {
		if (schedule.getBookingOpenAt() == null) {
			throw new BusinessException(PrereservationErrorCode.BOOKING_TIME_NOT_CONFIGURED);
		}
	}

	public record Slot(LocalDateTime startAt, LocalDateTime endAt) {
	}
}
