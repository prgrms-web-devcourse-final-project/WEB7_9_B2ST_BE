package com.back.b2st.domain.seatapplication.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.seatapplication.error.SeatSectionApplicationErrorCode;
import com.back.b2st.domain.seatapplication.repository.SeatSectionApplicationRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.scheduleseat.error.ScheduleSeatErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatBookingAccessService {

	private final PerformanceScheduleRepository performanceScheduleRepository;
	private final SeatRepository seatRepository;
	private final SeatSectionApplicationRepository seatSectionApplicationRepository;

	@Transactional(readOnly = true)
	public void validateSeatHoldAllowed(Long memberId, Long scheduleId, Long seatId) {
		PerformanceSchedule schedule = performanceScheduleRepository.findById(scheduleId)
			.orElseThrow(() -> new BusinessException(SeatSectionApplicationErrorCode.SCHEDULE_NOT_FOUND));

		if (schedule.getBookingType() != BookingType.SEAT) {
			return;
		}

		LocalDateTime now = LocalDateTime.now();
		if (schedule.getBookingOpenAt() != null && now.isBefore(schedule.getBookingOpenAt())) {
			throw new BusinessException(SeatSectionApplicationErrorCode.BOOKING_NOT_OPEN);
		}
		if (schedule.getBookingCloseAt() != null && now.isAfter(schedule.getBookingCloseAt())) {
			throw new BusinessException(SeatSectionApplicationErrorCode.BOOKING_CLOSED);
		}

		Seat seat = seatRepository.findById(seatId)
			.orElseThrow(() -> new BusinessException(ScheduleSeatErrorCode.SEAT_NOT_FOUND));

		boolean applied = seatSectionApplicationRepository.existsByPerformanceScheduleIdAndMemberIdAndSectionId(
			scheduleId,
			memberId,
			seat.getSectionId()
		);
		if (!applied) {
			throw new BusinessException(SeatSectionApplicationErrorCode.SECTION_NOT_ACTIVATED);
		}
	}
}
