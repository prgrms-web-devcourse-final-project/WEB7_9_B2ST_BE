package com.back.b2st.domain.prereservation.policy.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.prereservation.entry.error.PrereservationErrorCode;
import com.back.b2st.domain.prereservation.policy.entity.PrereservationTimeTable;
import com.back.b2st.domain.prereservation.policy.repository.PrereservationTimeTableRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class PrereservationSlotServiceTest {

	@Mock
	private PrereservationTimeTableRepository prereservationTimeTableRepository;

	@InjectMocks
	private PrereservationSlotService prereservationSlotService;

	private static final Long SCHEDULE_ID = 1L;
	private static final Long SECTION_ID = 10L;

	@Test
	@DisplayName("calculateSlotOrThrow(): 예매 시간이 설정되지 않으면 BOOKING_TIME_NOT_CONFIGURED 예외")
	void calculateSlotOrThrow_bookingTimeNotConfigured_throw() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		Section section = mock(Section.class);

		given(schedule.getBookingOpenAt()).willReturn(null);

		// when & then
		assertThatThrownBy(() -> prereservationSlotService.calculateSlotOrThrow(schedule, section))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.BOOKING_TIME_NOT_CONFIGURED.getMessage());

		then(prereservationTimeTableRepository).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("calculateSlotOrThrow(): 타임테이블이 없으면 TIME_TABLE_NOT_CONFIGURED 예외")
	void calculateSlotOrThrow_timeTableNotFound_throw() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		Section section = mock(Section.class);

		given(schedule.getPerformanceScheduleId()).willReturn(SCHEDULE_ID);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.of(2025, 1, 1, 14, 0));
		given(section.getId()).willReturn(SECTION_ID);

		given(prereservationTimeTableRepository.findTopByPerformanceScheduleIdAndSectionIdOrderByIdDesc(
			SCHEDULE_ID, SECTION_ID
		)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> prereservationSlotService.calculateSlotOrThrow(schedule, section))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.TIME_TABLE_NOT_CONFIGURED.getMessage());
	}

	@Test
	@DisplayName("calculateSlotOrThrow(): 타임테이블 조회 성공")
	void calculateSlotOrThrow_success() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		Section section = mock(Section.class);

		LocalDateTime bookingStartAt = LocalDateTime.of(2025, 1, 1, 14, 0);
		LocalDateTime bookingEndAt = LocalDateTime.of(2025, 1, 1, 15, 0);

		PrereservationTimeTable timeTable = mock(PrereservationTimeTable.class);
		given(timeTable.getBookingStartAt()).willReturn(bookingStartAt);
		given(timeTable.getBookingEndAt()).willReturn(bookingEndAt);

		given(schedule.getPerformanceScheduleId()).willReturn(SCHEDULE_ID);
		given(schedule.getBookingOpenAt()).willReturn(bookingStartAt);
		given(section.getId()).willReturn(SECTION_ID);

		given(prereservationTimeTableRepository.findTopByPerformanceScheduleIdAndSectionIdOrderByIdDesc(
			SCHEDULE_ID, SECTION_ID
		)).willReturn(Optional.of(timeTable));

		// when
		PrereservationSlotService.Slot result = prereservationSlotService.calculateSlotOrThrow(schedule, section);

		// then
		assertThat(result).isNotNull();
		assertThat(result.startAt()).isEqualTo(bookingStartAt);
		assertThat(result.endAt()).isEqualTo(bookingEndAt);
	}
}
