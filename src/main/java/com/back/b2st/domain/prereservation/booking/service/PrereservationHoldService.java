package com.back.b2st.domain.prereservation.booking.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.prereservation.entry.error.PrereservationErrorCode;
import com.back.b2st.domain.prereservation.entry.repository.PrereservationRepository;
import com.back.b2st.domain.prereservation.policy.service.PrereservationSlotService;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.domain.scheduleseat.error.ScheduleSeatErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrereservationHoldService {

	private final PerformanceScheduleRepository performanceScheduleRepository;
	private final SeatRepository seatRepository;
	private final SectionRepository sectionRepository;
	private final PrereservationRepository prereservationRepository;
	private final PrereservationSlotService prereservationSlotService;

	@Transactional(readOnly = true)
	public void validateSeatHoldAllowed(Long memberId, Long scheduleId, Long seatId) {
		PerformanceSchedule schedule = performanceScheduleRepository.findById(scheduleId)
			.orElseThrow(() -> new BusinessException(PrereservationErrorCode.SCHEDULE_NOT_FOUND));

		if (schedule.getBookingType() != BookingType.PRERESERVE) {
			return;
		}

		LocalDateTime bookingOpenAt = schedule.getBookingOpenAt();
		if (bookingOpenAt == null) {
			throw new BusinessException(PrereservationErrorCode.BOOKING_TIME_NOT_CONFIGURED);
		}

		LocalDateTime now = LocalDateTime.now();
		if (now.isBefore(bookingOpenAt)) {
			throw new BusinessException(PrereservationErrorCode.BOOKING_NOT_OPEN);
		}
		if (schedule.getBookingCloseAt() != null && now.isAfter(schedule.getBookingCloseAt())) {
			throw new BusinessException(PrereservationErrorCode.BOOKING_CLOSED);
		}

		Seat seat = seatRepository.findById(seatId)
			.orElseThrow(() -> new BusinessException(ScheduleSeatErrorCode.SEAT_NOT_FOUND));

		Long seatSectionId = seat.getSectionId();
		boolean applied = prereservationRepository.existsByPerformanceScheduleIdAndMemberIdAndSectionId(
			scheduleId,
			memberId,
			seatSectionId
		);
		if (!applied) {
			throw new BusinessException(PrereservationErrorCode.SECTION_NOT_ACTIVATED);
		}

		Section section = sectionRepository.findById(seatSectionId)
			.orElseThrow(() -> new BusinessException(PrereservationErrorCode.SECTION_NOT_FOUND));

		var slot = prereservationSlotService.calculateSlotOrThrow(schedule, section);
		if (now.isBefore(slot.startAt()) || now.isAfter(slot.endAt())) {
			throw new BusinessException(PrereservationErrorCode.BOOKING_SLOT_NOT_OPEN);
		}
	}
}
