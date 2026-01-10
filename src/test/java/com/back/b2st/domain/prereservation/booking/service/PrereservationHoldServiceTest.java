package com.back.b2st.domain.prereservation.booking.service;

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
import com.back.b2st.domain.prereservation.entry.error.PrereservationErrorCode;
import com.back.b2st.domain.prereservation.entry.repository.PrereservationRepository;
import com.back.b2st.domain.prereservation.policy.service.PrereservationSlotService;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.scheduleseat.error.ScheduleSeatErrorCode;
import com.back.b2st.domain.venue.section.entity.Section;
import com.back.b2st.domain.venue.section.repository.SectionRepository;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class PrereservationHoldServiceTest {

	@Mock
	private PerformanceScheduleRepository performanceScheduleRepository;

	@Mock
	private SeatRepository seatRepository;

	@Mock
	private SectionRepository sectionRepository;

	@Mock
	private PrereservationRepository prereservationRepository;

	@Mock
	private PrereservationSlotService prereservationSlotService;

	@InjectMocks
	private PrereservationHoldService prereservationHoldService;

	private static final Long MEMBER_ID = 1L;
	private static final Long SCHEDULE_ID = 10L;
	private static final Long SEAT_ID = 100L;
	private static final Long SECTION_ID = 7L;

	@Test
	@DisplayName("validateSeatHoldAllowed(): BookingType이 PRERESERVE가 아니면 BOOKING_TYPE_NOT_SUPPORTED 예외")
	void validateHold_skipNonPrereserve() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.FIRST_COME);
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		// when & then
		assertThatThrownBy(() -> prereservationHoldService.validateSeatHoldAllowed(MEMBER_ID, SCHEDULE_ID, SEAT_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.BOOKING_TYPE_NOT_SUPPORTED.getMessage());
		then(seatRepository).shouldHaveNoInteractions();
		then(prereservationRepository).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("validateSeatHoldAllowed(): 예매 오픈 전이면 BOOKING_NOT_OPEN 예외")
	void validateHold_beforeOpen() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().plusMinutes(10));
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		// when & then
		assertThatThrownBy(() -> prereservationHoldService.validateSeatHoldAllowed(MEMBER_ID, SCHEDULE_ID, SEAT_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.BOOKING_NOT_OPEN.getMessage());
	}

	@Test
	@DisplayName("validateSeatHoldAllowed(): 예매 마감 이후면 BOOKING_CLOSED 예외")
	void validateHold_afterClose() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().minusMinutes(10));
		given(schedule.getBookingCloseAt()).willReturn(LocalDateTime.now().minusMinutes(1));
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		// when & then
		assertThatThrownBy(() -> prereservationHoldService.validateSeatHoldAllowed(MEMBER_ID, SCHEDULE_ID, SEAT_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.BOOKING_CLOSED.getMessage());
	}

	@Test
	@DisplayName("validateSeatHoldAllowed(): 좌석이 없으면 SEAT_NOT_FOUND 예외")
	void validateHold_seatNotFound() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().minusMinutes(1));
		given(schedule.getBookingCloseAt()).willReturn(LocalDateTime.now().plusMinutes(10));
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(seatRepository.findById(SEAT_ID)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> prereservationHoldService.validateSeatHoldAllowed(MEMBER_ID, SCHEDULE_ID, SEAT_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(ScheduleSeatErrorCode.SEAT_NOT_FOUND.getMessage());
	}

	@Test
	@DisplayName("validateSeatHoldAllowed(): 신청하지 않은 구역이면 SECTION_NOT_ACTIVATED 예외")
	void validateHold_notApplied() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
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
		assertThatThrownBy(() -> prereservationHoldService.validateSeatHoldAllowed(MEMBER_ID, SCHEDULE_ID, SEAT_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.SECTION_NOT_ACTIVATED.getMessage());
	}

	@Test
	@DisplayName("validateSeatHoldAllowed(): 시간대가 아니면 BOOKING_SLOT_NOT_OPEN 예외")
	void validateHold_outOfSlot() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().minusHours(2));
		given(schedule.getBookingCloseAt()).willReturn(LocalDateTime.now().plusHours(10));
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		Seat seat = mock(Seat.class);
		given(seat.getSectionId()).willReturn(SECTION_ID);
		given(seatRepository.findById(SEAT_ID)).willReturn(Optional.of(seat));

		given(prereservationRepository.existsByPerformanceScheduleIdAndMemberIdAndSectionId(
			SCHEDULE_ID, MEMBER_ID, SECTION_ID
		)).willReturn(true);

		Section section = mock(Section.class);
		given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(section));

		given(prereservationSlotService.calculateSlotOrThrow(eq(schedule), eq(section)))
			.willReturn(new PrereservationSlotService.Slot(
				LocalDateTime.now().plusHours(1),
				LocalDateTime.now().plusHours(2)
			));

		// when & then
		assertThatThrownBy(() -> prereservationHoldService.validateSeatHoldAllowed(MEMBER_ID, SCHEDULE_ID, SEAT_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PrereservationErrorCode.BOOKING_SLOT_NOT_OPEN.getMessage());
	}

	@Test
	@DisplayName("validateSeatHoldAllowed(): 신청한 구역이고 시간대면 통과한다")
	void validateHold_success() {
		// given
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
		given(schedule.getBookingOpenAt()).willReturn(LocalDateTime.now().minusHours(2));
		given(schedule.getBookingCloseAt()).willReturn(LocalDateTime.now().plusHours(10));
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		Seat seat = mock(Seat.class);
		given(seat.getSectionId()).willReturn(SECTION_ID);
		given(seatRepository.findById(SEAT_ID)).willReturn(Optional.of(seat));

		given(prereservationRepository.existsByPerformanceScheduleIdAndMemberIdAndSectionId(
			SCHEDULE_ID, MEMBER_ID, SECTION_ID
		)).willReturn(true);

		Section section = mock(Section.class);
		given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(section));

		given(prereservationSlotService.calculateSlotOrThrow(eq(schedule), eq(section)))
			.willReturn(new PrereservationSlotService.Slot(
				LocalDateTime.now().minusMinutes(10),
				LocalDateTime.now().plusMinutes(10)
			));

		// when & then
		assertThatCode(() -> prereservationHoldService.validateSeatHoldAllowed(MEMBER_ID, SCHEDULE_ID, SEAT_ID))
			.doesNotThrowAnyException();
	}
}
