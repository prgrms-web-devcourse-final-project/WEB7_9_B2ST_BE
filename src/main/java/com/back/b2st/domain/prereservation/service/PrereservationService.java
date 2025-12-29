package com.back.b2st.domain.prereservation.service;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.prereservation.dto.response.PrereservationRes;
import com.back.b2st.domain.prereservation.entity.Prereservation;
import com.back.b2st.domain.prereservation.error.PrereservationErrorCode;
import com.back.b2st.domain.prereservation.repository.PrereservationRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.scheduleseat.error.ScheduleSeatErrorCode;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrereservationService {

	private final PerformanceScheduleRepository performanceScheduleRepository;
	private final SeatRepository seatRepository;
	private final SectionRepository sectionRepository;
	private final PrereservationRepository prereservationRepository;

	@Transactional
	public void apply(Long scheduleId, Long memberId, Long sectionId) {
		PerformanceSchedule schedule = performanceScheduleRepository.findById(scheduleId)
			.orElseThrow(() -> new BusinessException(PrereservationErrorCode.SCHEDULE_NOT_FOUND));

		if (schedule.getBookingType() != BookingType.PRERESERVE) {
			throw new BusinessException(PrereservationErrorCode.BOOKING_TYPE_NOT_SUPPORTED);
		}

		LocalDateTime now = LocalDateTime.now();
		if (schedule.getBookingOpenAt() != null && !now.isBefore(schedule.getBookingOpenAt())) {
			throw new BusinessException(PrereservationErrorCode.APPLICATION_CLOSED);
		}

		Section section = sectionRepository.findById(sectionId)
			.orElseThrow(() -> new BusinessException(PrereservationErrorCode.SECTION_NOT_FOUND));

		Long venueId = schedule.getPerformance().getVenue().getVenueId();
		if (!section.getVenueId().equals(venueId)) {
			throw new BusinessException(PrereservationErrorCode.SECTION_NOT_IN_VENUE);
		}

		if (prereservationRepository.existsByPerformanceScheduleIdAndMemberIdAndSectionId(
			scheduleId, memberId, sectionId)) {
			throw new BusinessException(PrereservationErrorCode.DUPLICATE_APPLICATION);
		}

		try {
			prereservationRepository.save(
				Prereservation.builder()
					.performanceScheduleId(scheduleId)
					.memberId(memberId)
					.sectionId(sectionId)
					.build()
			);
		} catch (DataIntegrityViolationException e) {
			throw new BusinessException(PrereservationErrorCode.DUPLICATE_APPLICATION);
		}
	}

	@Transactional(readOnly = true)
	public PrereservationRes getMyApplications(Long scheduleId, Long memberId) {
		var applications = prereservationRepository
			.findAllByPerformanceScheduleIdAndMemberIdOrderByCreatedAtDesc(scheduleId, memberId);
		var sectionIds = applications.stream().map(Prereservation::getSectionId).distinct().toList();
		return PrereservationRes.of(scheduleId, sectionIds);
	}

	@Transactional(readOnly = true)
	public void validateSeatHoldAllowed(Long memberId, Long scheduleId, Long seatId) {
		PerformanceSchedule schedule = performanceScheduleRepository.findById(scheduleId)
			.orElseThrow(() -> new BusinessException(PrereservationErrorCode.SCHEDULE_NOT_FOUND));

		if (schedule.getBookingType() != BookingType.PRERESERVE) {
			return;
		}

		LocalDateTime now = LocalDateTime.now();
		if (schedule.getBookingOpenAt() != null && now.isBefore(schedule.getBookingOpenAt())) {
			throw new BusinessException(PrereservationErrorCode.BOOKING_NOT_OPEN);
		}
		if (schedule.getBookingCloseAt() != null && now.isAfter(schedule.getBookingCloseAt())) {
			throw new BusinessException(PrereservationErrorCode.BOOKING_CLOSED);
		}

		Seat seat = seatRepository.findById(seatId)
			.orElseThrow(() -> new BusinessException(ScheduleSeatErrorCode.SEAT_NOT_FOUND));

		boolean applied = prereservationRepository.existsByPerformanceScheduleIdAndMemberIdAndSectionId(
			scheduleId,
			memberId,
			seat.getSectionId()
		);
		if (!applied) {
			throw new BusinessException(PrereservationErrorCode.SECTION_NOT_ACTIVATED);
		}
	}
}
