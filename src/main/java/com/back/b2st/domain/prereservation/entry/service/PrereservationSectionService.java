package com.back.b2st.domain.prereservation.entry.service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.prereservation.entry.dto.response.PrereservationSectionRes;
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
public class PrereservationSectionService {

	private final PerformanceScheduleRepository performanceScheduleRepository;
	private final SectionRepository sectionRepository;
	private final PrereservationRepository prereservationRepository;
	private final PrereservationSlotService prereservationSlotService;

	@Value("${prereservation.slot.strict:true}")
	private boolean slotStrict = true;

	@Transactional(readOnly = true)
	public List<PrereservationSectionRes> getSections(Long scheduleId, Long memberId) {
		PerformanceSchedule schedule = performanceScheduleRepository.findById(scheduleId)
			.orElseThrow(() -> new BusinessException(PrereservationErrorCode.SCHEDULE_NOT_FOUND));

		if (schedule.getBookingType() != BookingType.PRERESERVE) {
			throw new BusinessException(PrereservationErrorCode.BOOKING_TYPE_NOT_SUPPORTED);
		}

		Long venueId = schedule.getPerformance().getVenue().getVenueId();
		List<Section> sections = sectionRepository.findByVenueId(venueId)
			.stream()
			.sorted(Comparator.comparing(Section::getId))
			.toList();

		Set<Long> appliedSectionIds = prereservationRepository
			.findAllByPerformanceScheduleIdAndMemberIdOrderByCreatedAtDesc(scheduleId, memberId)
			.stream()
			.map(Prereservation::getSectionId)
			.collect(Collectors.toSet());

		return sections.stream()
			.map(section -> {
				var slot = slotStrict
					? prereservationSlotService.calculateSlotOrThrow(schedule, section)
					: new PrereservationSlotService.Slot(
						schedule.getBookingOpenAt(),
						schedule.getBookingCloseAt() != null
							? schedule.getBookingCloseAt()
							: schedule.getBookingOpenAt().plusDays(30)
					);
				return new PrereservationSectionRes(
					section.getId(),
					section.getSectionName(),
					slot.startAt(),
					slot.endAt(),
					appliedSectionIds.contains(section.getId())
				);
			})
			.toList();
	}
}
