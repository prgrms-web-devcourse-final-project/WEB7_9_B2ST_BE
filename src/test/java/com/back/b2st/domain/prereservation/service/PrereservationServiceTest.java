package com.back.b2st.domain.prereservation.service;

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

import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.prereservation.dto.response.PrereservationRes;
import com.back.b2st.domain.prereservation.entity.Prereservation;
import com.back.b2st.domain.prereservation.error.PrereservationErrorCode;
import com.back.b2st.domain.prereservation.repository.PrereservationRepository;
import com.back.b2st.domain.scheduleseat.error.ScheduleSeatErrorCode;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.global.error.exception.BusinessException;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class PrereservationServiceTest {

	@Mock
	private PerformanceScheduleRepository performanceScheduleRepository;

	@Mock
	private SeatRepository seatRepository;

	@Mock
	private SectionRepository sectionRepository;

	@Mock
	private PrereservationRepository prereservationRepository;

	@InjectMocks
	private PrereservationService prereservationService;

	private static final Long MEMBER_ID = 1L;
	private static final Long SCHEDULE_ID = 10L;
	private static final Long SEAT_ID = 100L;
	private static final Long SECTION_ID = 7L;

	@Test
	@DisplayName("validateSeatHoldAllowed(): BookingType이 SEAT이 아니면 검증을 스킵한다")
	void validateSeatHoldAllowed_skipsWhenNotSeatBooking() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.FIRST_COME);
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		// when & then
		assertThatCode(() -> prereservationService.validateSeatHoldAllowed(MEMBER_ID, SCHEDULE_ID, SEAT_ID))
			.doesNotThrowAnyException();
		then(seatRepository).shouldHaveNoInteractions();
		then(prereservationRepository).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("validateSeatHoldAllowed(): 예매 오픈 전이면 BOOKING_NOT_OPEN 예외")
	void validateSeatHoldAllowed_throwsWhenBeforeOpen() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.SEAT);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().plusMinutes(10));
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		// when & then
		assertThatThrownBy(() -> prereservationService.validateSeatHoldAllowed(MEMBER_ID, SCHEDULE_ID, SEAT_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.BOOKING_NOT_OPEN.getMessage());
	}

	@Test
	@DisplayName("validateSeatHoldAllowed(): 예매 마감 이후면 BOOKING_CLOSED 예외")
	void validateSeatHoldAllowed_throwsWhenAfterClose() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.SEAT);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().minusMinutes(10));
		given(schedule.getBookingCloseAt()).willReturn(LocalDateTime.now().minusMinutes(1));
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		// when & then
		assertThatThrownBy(() -> prereservationService.validateSeatHoldAllowed(MEMBER_ID, SCHEDULE_ID, SEAT_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.BOOKING_CLOSED.getMessage());
	}

	@Test
	@DisplayName("validateSeatHoldAllowed(): 좌석이 없으면 SEAT_NOT_FOUND 예외")
	void validateSeatHoldAllowed_throwsWhenSeatNotFound() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.SEAT);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().minusMinutes(1));
		given(schedule.getBookingCloseAt()).willReturn(LocalDateTime.now().plusMinutes(10));
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(seatRepository.findById(SEAT_ID)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> prereservationService.validateSeatHoldAllowed(MEMBER_ID, SCHEDULE_ID, SEAT_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(ScheduleSeatErrorCode.SEAT_NOT_FOUND.getMessage());
	}

	@Test
	@DisplayName("validateSeatHoldAllowed(): 신청하지 않은 구역이면 SECTION_NOT_ACTIVATED 예외")
	void validateSeatHoldAllowed_throwsWhenNotApplied() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.SEAT);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().minusMinutes(1));
		given(schedule.getBookingCloseAt()).willReturn(LocalDateTime.now().plusMinutes(10));
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		Seat seat = mock(Seat.class);
		given(seat.getSectionId()).willReturn(SECTION_ID);
		given(seatRepository.findById(SEAT_ID)).willReturn(Optional.of(seat));

		given(prereservationRepository.existsByPerformanceScheduleIdAndMemberIdAndSectionId(
			SCHEDULE_ID, MEMBER_ID, SECTION_ID
		)).willReturn(false);

		// when & then
		assertThatThrownBy(() -> prereservationService.validateSeatHoldAllowed(MEMBER_ID, SCHEDULE_ID, SEAT_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.SECTION_NOT_ACTIVATED.getMessage());
	}

	@Test
	@DisplayName("validateSeatHoldAllowed(): 신청한 구역이면 예외 없이 통과한다")
	void validateSeatHoldAllowed_allowsWhenApplied() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.SEAT);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().minusMinutes(1));
		given(schedule.getBookingCloseAt()).willReturn(LocalDateTime.now().plusMinutes(10));
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		Seat seat = mock(Seat.class);
		given(seat.getSectionId()).willReturn(SECTION_ID);
		given(seatRepository.findById(SEAT_ID)).willReturn(Optional.of(seat));

		given(prereservationRepository.existsByPerformanceScheduleIdAndMemberIdAndSectionId(
			SCHEDULE_ID, MEMBER_ID, SECTION_ID
		)).willReturn(true);

		// when & then
		assertThatCode(() -> prereservationService.validateSeatHoldAllowed(MEMBER_ID, SCHEDULE_ID, SEAT_ID))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("apply(): 공연 회차가 없으면 SCHEDULE_NOT_FOUND 예외")
	void apply_scheduleNotFound_throw() {
		// given
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> prereservationService.apply(SCHEDULE_ID, MEMBER_ID, SECTION_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.SCHEDULE_NOT_FOUND.getMessage());
	}

	@Test
	@DisplayName("apply(): BookingType이 SEAT이 아니면 BOOKING_TYPE_NOT_SUPPORTED 예외")
	void apply_notSeatBookingType_throw() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.FIRST_COME);
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		// when & then
		assertThatThrownBy(() -> prereservationService.apply(SCHEDULE_ID, MEMBER_ID, SECTION_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.BOOKING_TYPE_NOT_SUPPORTED.getMessage());
	}

	@Test
	@DisplayName("apply(): 예매 오픈 시간 이후면 APPLICATION_CLOSED 예외")
	void apply_afterBookingOpen_throw() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.SEAT);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().minusMinutes(1));
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		// when & then
		assertThatThrownBy(() -> prereservationService.apply(SCHEDULE_ID, MEMBER_ID, SECTION_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.APPLICATION_CLOSED.getMessage());
	}

	@Test
	@DisplayName("apply(): 구역이 없으면 SECTION_NOT_FOUND 예외")
	void apply_sectionNotFound_throw() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.SEAT);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().plusMinutes(10));
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> prereservationService.apply(SCHEDULE_ID, MEMBER_ID, SECTION_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.SECTION_NOT_FOUND.getMessage());
	}

	@Test
	@DisplayName("apply(): 구역이 공연장에 속하지 않으면 SECTION_NOT_IN_VENUE 예외")
	void apply_sectionNotInVenue_throw() {
		// given
		Long venueId = 100L;
		Long otherVenueId = 200L;

		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		Performance performance = mock(Performance.class);
		Venue venue = mock(Venue.class);
		Section section = mock(Section.class);

		given(schedule.getBookingType()).willReturn(BookingType.SEAT);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().plusMinutes(10));
		given(schedule.getPerformance()).willReturn(performance);
		given(performance.getVenue()).willReturn(venue);
		given(venue.getVenueId()).willReturn(venueId);

		given(section.getVenueId()).willReturn(otherVenueId);

		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(section));

		// when & then
		assertThatThrownBy(() -> prereservationService.apply(SCHEDULE_ID, MEMBER_ID, SECTION_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.SECTION_NOT_IN_VENUE.getMessage());
	}

	@Test
	@DisplayName("apply(): 이미 신청한 구역이면 DUPLICATE_APPLICATION 예외")
	void apply_alreadyApplied_throw() {
		// given
		Long venueId = 100L;

		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		Performance performance = mock(Performance.class);
		Venue venue = mock(Venue.class);
		Section section = mock(Section.class);

		given(schedule.getBookingType()).willReturn(BookingType.SEAT);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().plusMinutes(10));
		given(schedule.getPerformance()).willReturn(performance);
		given(performance.getVenue()).willReturn(venue);
		given(venue.getVenueId()).willReturn(venueId);

		given(section.getVenueId()).willReturn(venueId);

		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(section));
		given(prereservationRepository.existsByPerformanceScheduleIdAndMemberIdAndSectionId(
			SCHEDULE_ID, MEMBER_ID, SECTION_ID
		)).willReturn(true);

		// when & then
		assertThatThrownBy(() -> prereservationService.apply(SCHEDULE_ID, MEMBER_ID, SECTION_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.DUPLICATE_APPLICATION.getMessage());
	}

	@Test
	@DisplayName("apply(): 정상적으로 신청이 저장된다")
	void apply_success() {
		// given
		Long venueId = 100L;

		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		Performance performance = mock(Performance.class);
		Venue venue = mock(Venue.class);
		Section section = mock(Section.class);

		given(schedule.getBookingType()).willReturn(BookingType.SEAT);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().plusMinutes(10));
		given(schedule.getPerformance()).willReturn(performance);
		given(performance.getVenue()).willReturn(venue);
		given(venue.getVenueId()).willReturn(venueId);

		given(section.getVenueId()).willReturn(venueId);

		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(section));
		given(prereservationRepository.existsByPerformanceScheduleIdAndMemberIdAndSectionId(
			SCHEDULE_ID, MEMBER_ID, SECTION_ID
		)).willReturn(false);

		// when & then
		assertThatCode(() -> prereservationService.apply(SCHEDULE_ID, MEMBER_ID, SECTION_ID))
			.doesNotThrowAnyException();

		then(prereservationRepository).should().save(any(Prereservation.class));
	}

	@Test
	@DisplayName("getMyApplications(): 신청한 구역 목록을 조회한다")
	void getMyApplications_success() {
		// given
		Prereservation prereservation1 = mock(Prereservation.class);
		Prereservation prereservation2 = mock(Prereservation.class);

		given(prereservation1.getSectionId()).willReturn(1L);
		given(prereservation2.getSectionId()).willReturn(2L);

		given(prereservationRepository.findAllByPerformanceScheduleIdAndMemberIdOrderByCreatedAtDesc(
			SCHEDULE_ID, MEMBER_ID
		)).willReturn(List.of(prereservation1, prereservation2));

		// when
		PrereservationRes result = prereservationService.getMyApplications(SCHEDULE_ID, MEMBER_ID);

		// then
		assertThat(result.scheduleId()).isEqualTo(SCHEDULE_ID);
		assertThat(result.sectionIds()).containsExactly(1L, 2L);
	}
}
