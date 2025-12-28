package com.back.b2st.domain.seatapplication.service;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.seatapplication.dto.response.SeatSectionApplicationRes;
import com.back.b2st.domain.seatapplication.entity.SeatSectionApplication;
import com.back.b2st.domain.seatapplication.error.SeatSectionApplicationErrorCode;
import com.back.b2st.domain.seatapplication.repository.SeatSectionApplicationRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatSectionApplicationService {

	private final PerformanceScheduleRepository performanceScheduleRepository;
	private final SectionRepository sectionRepository;
	private final SeatSectionApplicationRepository seatSectionApplicationRepository;

	@Transactional
	public void apply(Long scheduleId, Long memberId, Long sectionId) {
		PerformanceSchedule schedule = performanceScheduleRepository.findById(scheduleId)
			.orElseThrow(() -> new BusinessException(SeatSectionApplicationErrorCode.SCHEDULE_NOT_FOUND));

		if (schedule.getBookingType() != BookingType.SEAT) {
			throw new BusinessException(SeatSectionApplicationErrorCode.BOOKING_TYPE_NOT_SUPPORTED);
		}

		LocalDateTime now = LocalDateTime.now();
		if (schedule.getBookingOpenAt() != null && !now.isBefore(schedule.getBookingOpenAt())) {
			throw new BusinessException(SeatSectionApplicationErrorCode.APPLICATION_CLOSED);
		}

		Section section = sectionRepository.findById(sectionId)
			.orElseThrow(() -> new BusinessException(SeatSectionApplicationErrorCode.SECTION_NOT_FOUND));

		Long venueId = schedule.getPerformance().getVenue().getVenueId();
		if (!section.getVenueId().equals(venueId)) {
			throw new BusinessException(SeatSectionApplicationErrorCode.SECTION_NOT_IN_VENUE);
		}

		if (seatSectionApplicationRepository.existsByPerformanceScheduleIdAndMemberIdAndSectionId(
			scheduleId, memberId, sectionId)) {
			throw new BusinessException(SeatSectionApplicationErrorCode.DUPLICATE_APPLICATION);
		}

		try {
			seatSectionApplicationRepository.save(
				SeatSectionApplication.builder()
					.performanceScheduleId(scheduleId)
					.memberId(memberId)
					.sectionId(sectionId)
					.build()
			);
		} catch (DataIntegrityViolationException e) {
			throw new BusinessException(SeatSectionApplicationErrorCode.DUPLICATE_APPLICATION);
		}
	}

	@Transactional(readOnly = true)
	public SeatSectionApplicationRes getMyApplications(Long scheduleId, Long memberId) {
		var applications = seatSectionApplicationRepository
			.findAllByPerformanceScheduleIdAndMemberIdOrderByCreatedAtDesc(scheduleId, memberId);
		var sectionIds = applications.stream().map(SeatSectionApplication::getSectionId).distinct().toList();
		return SeatSectionApplicationRes.of(scheduleId, sectionIds);
	}
}
