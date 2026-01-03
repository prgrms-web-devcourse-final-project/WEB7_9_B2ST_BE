package com.back.b2st.domain.prereservation.policy.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.prereservation.entry.error.PrereservationErrorCode;
import com.back.b2st.domain.prereservation.policy.dto.request.PrereservationTimeTableUpsertReq;
import com.back.b2st.domain.prereservation.policy.entity.PrereservationTimeTable;
import com.back.b2st.domain.prereservation.policy.repository.PrereservationTimeTableRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrereservationTimeTableService {

	private final PerformanceScheduleRepository performanceScheduleRepository;
	private final SectionRepository sectionRepository;
	private final PrereservationTimeTableRepository prereservationTimeTableRepository;

	@Transactional(readOnly = true)
	public List<PrereservationTimeTable> getTimeTables(Long scheduleId) {
		validatePrereserveScheduleOrThrow(scheduleId);
		return prereservationTimeTableRepository
			.findAllByPerformanceScheduleIdOrderByBookingStartAtAscSectionIdAsc(scheduleId);
	}

	@Transactional
	public void upsert(Long scheduleId, List<PrereservationTimeTableUpsertReq> items) {
		PerformanceSchedule schedule = validatePrereserveScheduleOrThrow(scheduleId);

		for (PrereservationTimeTableUpsertReq item : items) {
			Section section = sectionRepository.findById(item.sectionId())
				.orElseThrow(() -> new BusinessException(PrereservationErrorCode.SECTION_NOT_FOUND));

			Long venueId = schedule.getPerformance().getVenue().getVenueId();
			if (!section.getVenueId().equals(venueId)) {
				throw new BusinessException(PrereservationErrorCode.SECTION_NOT_IN_VENUE);
			}

			validateTimeRangeOrThrow(item.bookingStartAt(), item.bookingEndAt());

			PrereservationTimeTable timeTable = prereservationTimeTableRepository
				.findByPerformanceScheduleIdAndSectionId(scheduleId, item.sectionId())
				.orElseGet(() -> PrereservationTimeTable.builder()
					.performanceScheduleId(scheduleId)
					.sectionId(item.sectionId())
					.bookingStartAt(item.bookingStartAt())
					.bookingEndAt(item.bookingEndAt())
					.build());

			timeTable.updateBookingTime(item.bookingStartAt(), item.bookingEndAt());
			prereservationTimeTableRepository.save(timeTable);
		}
	}

	private PerformanceSchedule validatePrereserveScheduleOrThrow(Long scheduleId) {
		PerformanceSchedule schedule = performanceScheduleRepository.findById(scheduleId)
			.orElseThrow(() -> new BusinessException(PrereservationErrorCode.SCHEDULE_NOT_FOUND));

		if (schedule.getBookingType() != BookingType.PRERESERVE) {
			throw new BusinessException(PrereservationErrorCode.BOOKING_TYPE_NOT_SUPPORTED);
		}

		if (schedule.getBookingOpenAt() == null) {
			throw new BusinessException(PrereservationErrorCode.BOOKING_TIME_NOT_CONFIGURED);
		}

		return schedule;
	}

	private void validateTimeRangeOrThrow(LocalDateTime startAt, LocalDateTime endAt) {
		if (startAt.isAfter(endAt) || startAt.isEqual(endAt)) {
			throw new BusinessException(PrereservationErrorCode.TIME_TABLE_NOT_CONFIGURED);
		}
	}
}
