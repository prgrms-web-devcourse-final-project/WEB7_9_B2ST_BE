package com.back.b2st.domain.prereservation.entry.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.prereservation.entry.dto.response.PrereservationSectionRes;
import com.back.b2st.domain.prereservation.entry.entity.Prereservation;
import com.back.b2st.domain.prereservation.entry.error.PrereservationErrorCode;
import com.back.b2st.domain.prereservation.entry.repository.PrereservationRepository;
import com.back.b2st.domain.prereservation.policy.service.PrereservationSlotService;
import com.back.b2st.domain.prereservation.policy.service.PrereservationTimeTableService;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class PrereservationSectionServiceTest {

	@Mock
	private PerformanceScheduleRepository performanceScheduleRepository;

	@Mock
	private SectionRepository sectionRepository;

	@Mock
	private PrereservationRepository prereservationRepository;

	@Mock
	private PrereservationSlotService prereservationSlotService;

	@Mock
	private PrereservationTimeTableService prereservationTimeTableService;

	@InjectMocks
	private PrereservationSectionService prereservationSectionService;

	private static final Long SCHEDULE_ID = 1L;
	private static final Long MEMBER_ID = 10L;
	private static final Long VENUE_ID = 100L;

	@Test
	@DisplayName("getSections(): 스케줄이 없으면 SCHEDULE_NOT_FOUND 예외")
	void getSections_scheduleNotFound_throw() {
		// given
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> prereservationSectionService.getSections(SCHEDULE_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.SCHEDULE_NOT_FOUND.getMessage());
	}

	@Test
	@DisplayName("getSections(): BookingType이 PRERESERVE가 아니면 BOOKING_TYPE_NOT_SUPPORTED 예외")
	void getSections_wrongBookingType_throw() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.FIRST_COME);
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		// when & then
		assertThatThrownBy(() -> prereservationSectionService.getSections(SCHEDULE_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.BOOKING_TYPE_NOT_SUPPORTED.getMessage());
	}

	@Test
	@DisplayName("getSections(): 구역 목록 조회 성공 (사전 신청 없음)")
	void getSections_noApplications_success() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		Performance performance = mock(Performance.class);
		Venue venue = mock(Venue.class);
		Section section1 = createSection(1L, "A구역");
		Section section2 = createSection(2L, "B구역");

		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
		given(schedule.getPerformance()).willReturn(performance);
		given(performance.getVenue()).willReturn(venue);
		given(venue.getVenueId()).willReturn(VENUE_ID);

		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(sectionRepository.findByVenueId(VENUE_ID)).willReturn(List.of(section1, section2));
		given(prereservationRepository.findAllByPerformanceScheduleIdAndMemberIdOrderByCreatedAtDesc(
			SCHEDULE_ID, MEMBER_ID
		)).willReturn(List.of());

		LocalDateTime startAt = LocalDateTime.of(2025, 1, 1, 14, 0);
		LocalDateTime endAt = LocalDateTime.of(2025, 1, 1, 15, 0);
		PrereservationSlotService.Slot slot = new PrereservationSlotService.Slot(startAt, endAt);

		given(prereservationSlotService.calculateSlotOrThrow(schedule, section1)).willReturn(slot);
		given(prereservationSlotService.calculateSlotOrThrow(schedule, section2)).willReturn(slot);

		// when
		List<PrereservationSectionRes> result = prereservationSectionService.getSections(SCHEDULE_ID, MEMBER_ID);

		// then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).sectionId()).isEqualTo(1L);
		assertThat(result.get(0).sectionName()).isEqualTo("A구역");
		assertThat(result.get(0).applied()).isFalse();
		assertThat(result.get(1).sectionId()).isEqualTo(2L);
		assertThat(result.get(1).sectionName()).isEqualTo("B구역");
		assertThat(result.get(1).applied()).isFalse();
	}

	@Test
	@DisplayName("getSections(): 구역 목록 조회 성공 (일부 구역 신청됨)")
	void getSections_someApplications_success() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		Performance performance = mock(Performance.class);
		Venue venue = mock(Venue.class);
		Section section1 = createSection(1L, "A구역");
		Section section2 = createSection(2L, "B구역");
		Section section3 = createSection(3L, "C구역");

		Prereservation prereservation = mock(Prereservation.class);
		given(prereservation.getSectionId()).willReturn(2L);

		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
		given(schedule.getPerformance()).willReturn(performance);
		given(performance.getVenue()).willReturn(venue);
		given(venue.getVenueId()).willReturn(VENUE_ID);

		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(sectionRepository.findByVenueId(VENUE_ID)).willReturn(List.of(section1, section2, section3));
		given(prereservationRepository.findAllByPerformanceScheduleIdAndMemberIdOrderByCreatedAtDesc(
			SCHEDULE_ID, MEMBER_ID
		)).willReturn(List.of(prereservation));

		LocalDateTime startAt = LocalDateTime.of(2025, 1, 1, 14, 0);
		LocalDateTime endAt = LocalDateTime.of(2025, 1, 1, 15, 0);
		PrereservationSlotService.Slot slot = new PrereservationSlotService.Slot(startAt, endAt);

		given(prereservationSlotService.calculateSlotOrThrow(schedule, section1)).willReturn(slot);
		given(prereservationSlotService.calculateSlotOrThrow(schedule, section2)).willReturn(slot);
		given(prereservationSlotService.calculateSlotOrThrow(schedule, section3)).willReturn(slot);

		// when
		List<PrereservationSectionRes> result = prereservationSectionService.getSections(SCHEDULE_ID, MEMBER_ID);

		// then
		assertThat(result).hasSize(3);
		assertThat(result.get(0).applied()).isFalse(); // section1
		assertThat(result.get(1).applied()).isTrue();  // section2 - 신청됨
		assertThat(result.get(2).applied()).isFalse(); // section3
	}

	private Section createSection(Long id, String name) {
		Section section = mock(Section.class);
		given(section.getId()).willReturn(id);
		given(section.getSectionName()).willReturn(name);
		return section;
	}
}
