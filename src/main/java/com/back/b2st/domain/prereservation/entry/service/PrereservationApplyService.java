package com.back.b2st.domain.prereservation.entry.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.email.service.EmailSender;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.prereservation.entry.dto.response.PrereservationRes;
import com.back.b2st.domain.prereservation.entry.entity.Prereservation;
import com.back.b2st.domain.prereservation.entry.error.PrereservationErrorCode;
import com.back.b2st.domain.prereservation.entry.repository.PrereservationRepository;
import com.back.b2st.domain.prereservation.policy.service.PrereservationSlotService;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrereservationApplyService {

	private static final DateTimeFormatter EMAIL_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private final PerformanceScheduleRepository performanceScheduleRepository;
	private final SectionRepository sectionRepository;
	private final PrereservationRepository prereservationRepository;
	private final PrereservationSlotService prereservationSlotService;
	private final EmailSender emailSender;

	@Value("${prereservation.application.strict:true}")
	private boolean applicationStrict = true;

	@Value("${app.frontend.my-page-url:https://doncrytt.vercel.app/my-page}")
	private String myPageUrl = "https://doncrytt.vercel.app/my-page";

	@Transactional
	public void apply(Long scheduleId, Long memberId, String email, Long sectionId) {
		PerformanceSchedule schedule = getScheduleOrThrow(scheduleId);

		if (schedule.getBookingType() != BookingType.PRERESERVE) {
			throw new BusinessException(PrereservationErrorCode.BOOKING_TYPE_NOT_SUPPORTED);
		}

		LocalDateTime bookingOpenAt = schedule.getBookingOpenAt();
		if (bookingOpenAt == null) {
			throw new BusinessException(PrereservationErrorCode.BOOKING_TIME_NOT_CONFIGURED);
		}

		if (applicationStrict) {
			LocalDateTime now = LocalDateTime.now();
			LocalDateTime applyOpenAt = bookingOpenAt.minusDays(1);
			if (now.isBefore(applyOpenAt)) {
				throw new BusinessException(PrereservationErrorCode.APPLICATION_NOT_OPEN);
			}
			if (!now.isBefore(bookingOpenAt)) {
				throw new BusinessException(PrereservationErrorCode.APPLICATION_CLOSED);
			}
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

		if (email != null && !email.isBlank()) {
			var slot = prereservationSlotService.calculateSlotOrThrow(schedule, section);
			sendAppliedEmail(email, section.getSectionName(), slot.startAt(), slot.endAt());
		}
	}

	@Transactional(readOnly = true)
	public PrereservationRes getMyApplications(Long scheduleId, Long memberId) {
		PerformanceSchedule schedule = getScheduleOrThrow(scheduleId);

		var applications = prereservationRepository
			.findAllByPerformanceScheduleIdAndMemberIdOrderByCreatedAtDesc(scheduleId, memberId);
		var sectionIds = applications.stream().map(Prereservation::getSectionId).distinct().toList();
		return PrereservationRes.of(
			scheduleId,
			sectionIds,
			schedule.getBookingOpenAt(),
			schedule.getBookingCloseAt()
		);
	}

	@Transactional(readOnly = true)
	public List<PrereservationRes> getMyApplicationList(Long memberId) {
		var applications = prereservationRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId);

		var sectionIdsByScheduleId = new TreeMap<Long, TreeSet<Long>>();
		for (Prereservation application : applications) {
			sectionIdsByScheduleId
				.computeIfAbsent(application.getPerformanceScheduleId(), ignored -> new TreeSet<>())
				.add(application.getSectionId());
		}

		Map<Long, PerformanceSchedule> scheduleById = performanceScheduleRepository
			.findAllById(sectionIdsByScheduleId.keySet())
			.stream()
			.collect(java.util.stream.Collectors.toMap(
				PerformanceSchedule::getPerformanceScheduleId,
				schedule -> schedule
			));

		var response = new ArrayList<PrereservationRes>(sectionIdsByScheduleId.size());
		for (var entry : sectionIdsByScheduleId.entrySet()) {
			PerformanceSchedule schedule = scheduleById.get(entry.getKey());
			if (schedule == null || schedule.getBookingType() != BookingType.PRERESERVE) {
				continue;
			}
			response.add(PrereservationRes.of(
				entry.getKey(),
				new ArrayList<>(entry.getValue()),
				schedule.getBookingOpenAt(),
				schedule.getBookingCloseAt()
			));
		}
		return response;
	}

	private PerformanceSchedule getScheduleOrThrow(Long scheduleId) {
		return performanceScheduleRepository.findById(scheduleId)
			.orElseThrow(() -> new BusinessException(PrereservationErrorCode.SCHEDULE_NOT_FOUND));
	}

	private void sendAppliedEmail(String email, String sectionName, LocalDateTime startAt, LocalDateTime endAt) {
		String message = """
			신청 예매 사전 신청이 완료되었습니다.

			- 신청 구역: %s
			- 예매 가능 시간: %s ~ %s

			해당 시간 외에는 예매가 불가능합니다.
			""".formatted(
			sectionName,
			startAt.format(EMAIL_TIME_FORMATTER),
			endAt.format(EMAIL_TIME_FORMATTER)
		);

		emailSender.sendNotificationEmail(
			email,
			"[TT] 신청 예매 사전 신청 완료",
			message,
			"예매 바로가기",
			myPageUrl
		);
	}
}
