package com.back.b2st.domain.prereservation.policy.service;

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
import com.back.b2st.domain.prereservation.entry.error.PrereservationErrorCode;
import com.back.b2st.domain.prereservation.policy.dto.request.PrereservationTimeTableUpsertReq;
import com.back.b2st.domain.prereservation.policy.entity.PrereservationTimeTable;
import com.back.b2st.domain.prereservation.policy.repository.PrereservationTimeTableRepository;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class PrereservationTimeTableServiceTest {

	@Mock
	private PerformanceScheduleRepository performanceScheduleRepository;

	@Mock
	private SectionRepository sectionRepository;

	@Mock
	private PrereservationTimeTableRepository prereservationTimeTableRepository;

	@InjectMocks
	private PrereservationTimeTableService prereservationTimeTableService;

	private static final Long SCHEDULE_ID = 1L;
	private static final Long VENUE_ID = 10L;
	private static final Long SECTION_ID = 100L;

	@Test
	@DisplayName("getTimeTables(): 신청예매 회차가 아니면 BOOKING_TYPE_NOT_SUPPORTED 예외")
	void getTimeTables_nonPrereserve_throw() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.FIRST_COME);
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		// when & then
		assertThatThrownBy(() -> prereservationTimeTableService.getTimeTables(SCHEDULE_ID))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException)ex).getErrorCode())
				.isEqualTo(PrereservationErrorCode.BOOKING_TYPE_NOT_SUPPORTED));
	}

	@Test
	@DisplayName("upsert(): 신규 타임테이블은 생성된다")
	void upsert_createsNew() {
		// given
		PerformanceSchedule schedule = mockPrereserveSchedule();
		Section section = mock(Section.class);

		LocalDateTime startAt = LocalDateTime.now().plusHours(1);
		LocalDateTime endAt = startAt.plusHours(1);
		var req = new PrereservationTimeTableUpsertReq(SECTION_ID, startAt, endAt);

		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(section));
		given(section.getVenueId()).willReturn(VENUE_ID);
		given(prereservationTimeTableRepository.findByPerformanceScheduleIdAndSectionId(SCHEDULE_ID, SECTION_ID))
			.willReturn(Optional.empty());

		// when
		prereservationTimeTableService.upsert(SCHEDULE_ID, List.of(req));

		// then
		then(prereservationTimeTableRepository).should().save(any(PrereservationTimeTable.class));
	}

	@Test
	@DisplayName("upsert(): 기존 타임테이블은 업데이트된다")
	void upsert_updatesExisting() {
		// given
		PerformanceSchedule schedule = mockPrereserveSchedule();
		Section section = mock(Section.class);

		LocalDateTime startAt = LocalDateTime.now().plusHours(1);
		LocalDateTime endAt = startAt.plusHours(1);
		var req = new PrereservationTimeTableUpsertReq(SECTION_ID, startAt, endAt);

		PrereservationTimeTable existing = PrereservationTimeTable.builder()
			.performanceScheduleId(SCHEDULE_ID)
			.sectionId(SECTION_ID)
			.bookingStartAt(startAt.minusHours(1))
			.bookingEndAt(endAt.minusHours(1))
			.build();

		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(section));
		given(section.getVenueId()).willReturn(VENUE_ID);
		given(prereservationTimeTableRepository.findByPerformanceScheduleIdAndSectionId(SCHEDULE_ID, SECTION_ID))
			.willReturn(Optional.of(existing));

		// when
		prereservationTimeTableService.upsert(SCHEDULE_ID, List.of(req));

		// then
		assertThat(existing.getBookingStartAt()).isEqualTo(startAt);
		assertThat(existing.getBookingEndAt()).isEqualTo(endAt);
		then(prereservationTimeTableRepository).should().save(existing);
	}

	@Test
	@DisplayName("upsert(): 구역이 공연장에 속하지 않으면 SECTION_NOT_IN_VENUE 예외")
	void upsert_sectionNotInVenue_throw() {
		// given
		PerformanceSchedule schedule = mockPrereserveSchedule();
		Section section = mock(Section.class);

		LocalDateTime startAt = LocalDateTime.now().plusHours(1);
		LocalDateTime endAt = startAt.plusHours(1);
		var req = new PrereservationTimeTableUpsertReq(SECTION_ID, startAt, endAt);

		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(section));
		given(section.getVenueId()).willReturn(999L);

		// when & then
		assertThatThrownBy(() -> prereservationTimeTableService.upsert(SCHEDULE_ID, List.of(req)))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException)ex).getErrorCode())
				.isEqualTo(PrereservationErrorCode.SECTION_NOT_IN_VENUE));
	}

	@Test
	@DisplayName("upsert(): 시작/종료 시간이 잘못되면 TIME_TABLE_NOT_CONFIGURED 예외")
	void upsert_invalidTimeRange_throw() {
		// given
		PerformanceSchedule schedule = mockPrereserveSchedule();
		Section section = mock(Section.class);

		LocalDateTime startAt = LocalDateTime.now().plusHours(2);
		LocalDateTime endAt = startAt.minusMinutes(1);
		var req = new PrereservationTimeTableUpsertReq(SECTION_ID, startAt, endAt);

		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(section));
		given(section.getVenueId()).willReturn(VENUE_ID);

		// when & then
		assertThatThrownBy(() -> prereservationTimeTableService.upsert(SCHEDULE_ID, List.of(req)))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException)ex).getErrorCode())
				.isEqualTo(PrereservationErrorCode.TIME_TABLE_NOT_CONFIGURED));
	}

	private PerformanceSchedule mockPrereserveSchedule() {
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		Performance performance = mock(Performance.class);
		Venue venue = mock(Venue.class);

		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().plusDays(1));
		given(schedule.getPerformance()).willReturn(performance);
		given(performance.getVenue()).willReturn(venue);
		given(venue.getVenueId()).willReturn(VENUE_ID);

		return schedule;
	}
}

