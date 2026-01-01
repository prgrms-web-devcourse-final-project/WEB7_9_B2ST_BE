package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.prereservation.booking.entity.PrereservationBooking;
import com.back.b2st.domain.prereservation.booking.entity.PrereservationBookingStatus;
import com.back.b2st.domain.prereservation.booking.service.PrereservationBookingService;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.scheduleseat.service.SeatHoldTokenService;
import com.back.b2st.domain.seat.grade.entity.SeatGrade;
import com.back.b2st.domain.seat.grade.repository.SeatGradeRepository;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class PrereservationPaymentHandlerTest {

	@Mock
	private ScheduleSeatRepository scheduleSeatRepository;

	@Mock
	private SeatHoldTokenService seatHoldTokenService;

	@Mock
	private PerformanceScheduleRepository performanceScheduleRepository;

	@Mock
	private SeatGradeRepository seatGradeRepository;

	@Mock
	private PrereservationBookingService prereservationBookingService;

	@InjectMocks
	private PrereservationPaymentHandler prereservationPaymentHandler;

	private static final Long BOOKING_ID = 1L;
	private static final Long MEMBER_ID = 10L;
	private static final Long SCHEDULE_ID = 100L;
	private static final Long SEAT_ID = 1000L;
	private static final Long SCHEDULE_SEAT_ID = 999L;
	private static final Long PERFORMANCE_ID = 50L;

	@Test
	@DisplayName("supports(): DomainType.PRERESERVATION 지원")
	void supports_prereservation_true() {
		assertThat(prereservationPaymentHandler.supports(DomainType.PRERESERVATION)).isTrue();
	}

	@Test
	@DisplayName("supports(): 다른 도메인 타입은 미지원")
	void supports_others_false() {
		assertThat(prereservationPaymentHandler.supports(DomainType.RESERVATION)).isFalse();
		assertThat(prereservationPaymentHandler.supports(DomainType.LOTTERY)).isFalse();
		assertThat(prereservationPaymentHandler.supports(DomainType.TRADE)).isFalse();
	}

	@Test
	@DisplayName("loadAndValidate(): booking이 없으면 DOMAIN_NOT_FOUND 예외")
	void loadAndValidate_bookingNotFound_throw() {
		given(prereservationBookingService.getBookingOrThrow(BOOKING_ID))
			.willThrow(new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND));

		assertThatThrownBy(() -> prereservationPaymentHandler.loadAndValidate(BOOKING_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PaymentErrorCode.DOMAIN_NOT_FOUND.getMessage());
	}

	@Test
	@DisplayName("loadAndValidate(): 다른 사용자의 booking이면 UNAUTHORIZED_PAYMENT_ACCESS 예외")
	void loadAndValidate_unauthorizedMember_throw() {
		PrereservationBooking booking = mock(PrereservationBooking.class);
		given(booking.getMemberId()).willReturn(999L);
		given(prereservationBookingService.getBookingOrThrow(BOOKING_ID)).willReturn(booking);

		assertThatThrownBy(() -> prereservationPaymentHandler.loadAndValidate(BOOKING_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS.getMessage());
	}

	@Test
	@DisplayName("loadAndValidate(): booking 상태가 CREATED가 아니면 DOMAIN_NOT_PAYABLE 예외")
	void loadAndValidate_notPayableStatus_throw() {
		PrereservationBooking booking = mock(PrereservationBooking.class);
		given(booking.getMemberId()).willReturn(MEMBER_ID);
		given(booking.getStatus()).willReturn(PrereservationBookingStatus.COMPLETED);
		given(prereservationBookingService.getBookingOrThrow(BOOKING_ID)).willReturn(booking);

		assertThatThrownBy(() -> prereservationPaymentHandler.loadAndValidate(BOOKING_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PaymentErrorCode.DOMAIN_NOT_PAYABLE.getMessage());
	}

	@Test
	@DisplayName("loadAndValidate(): scheduleSeat가 없으면 DOMAIN_NOT_FOUND 예외")
	void loadAndValidate_scheduleSeatNotFound_throw() {
		PrereservationBooking booking = mock(PrereservationBooking.class);
		given(booking.getMemberId()).willReturn(MEMBER_ID);
		given(booking.getStatus()).willReturn(PrereservationBookingStatus.CREATED);
		given(booking.getScheduleSeatId()).willReturn(SCHEDULE_SEAT_ID);
		given(prereservationBookingService.getBookingOrThrow(BOOKING_ID)).willReturn(booking);
		given(scheduleSeatRepository.findById(SCHEDULE_SEAT_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> prereservationPaymentHandler.loadAndValidate(BOOKING_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PaymentErrorCode.DOMAIN_NOT_FOUND.getMessage());
	}

	@Test
	@DisplayName("loadAndValidate(): BookingType이 PRERESERVE가 아니면 DOMAIN_NOT_PAYABLE 예외")
	void loadAndValidate_wrongBookingType_throw() {
		PrereservationBooking booking = mock(PrereservationBooking.class);
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		ScheduleSeat scheduleSeat = mock(ScheduleSeat.class);

		given(booking.getMemberId()).willReturn(MEMBER_ID);
		given(booking.getStatus()).willReturn(PrereservationBookingStatus.CREATED);
		given(booking.getScheduleSeatId()).willReturn(SCHEDULE_SEAT_ID);
		given(prereservationBookingService.getBookingOrThrow(BOOKING_ID)).willReturn(booking);

		given(scheduleSeatRepository.findById(SCHEDULE_SEAT_ID)).willReturn(Optional.of(scheduleSeat));
		given(scheduleSeat.getScheduleId()).willReturn(SCHEDULE_ID);

		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(schedule.getBookingType()).willReturn(BookingType.FIRST_COME);

		assertThatThrownBy(() -> prereservationPaymentHandler.loadAndValidate(BOOKING_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("신청 예매 결제 대상이 아닙니다");
	}

	@Test
	@DisplayName("loadAndValidate(): 좌석이 HOLD 상태가 아니면 DOMAIN_NOT_PAYABLE 예외")
	void loadAndValidate_seatNotHold_throw() {
		PrereservationBooking booking = mock(PrereservationBooking.class);
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		ScheduleSeat scheduleSeat = mock(ScheduleSeat.class);

		given(booking.getMemberId()).willReturn(MEMBER_ID);
		given(booking.getStatus()).willReturn(PrereservationBookingStatus.CREATED);
		given(booking.getScheduleSeatId()).willReturn(SCHEDULE_SEAT_ID);
		given(prereservationBookingService.getBookingOrThrow(BOOKING_ID)).willReturn(booking);

		given(scheduleSeatRepository.findById(SCHEDULE_SEAT_ID)).willReturn(Optional.of(scheduleSeat));
		given(scheduleSeat.getScheduleId()).willReturn(SCHEDULE_ID);
		given(scheduleSeat.getStatus()).willReturn(SeatStatus.AVAILABLE);

		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);

		assertThatThrownBy(() -> prereservationPaymentHandler.loadAndValidate(BOOKING_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PaymentErrorCode.DOMAIN_NOT_PAYABLE.getMessage());
	}

	@Test
	@DisplayName("loadAndValidate(): 정상 케이스 - PaymentTarget 반환")
	void loadAndValidate_success() {
		PrereservationBooking booking = mock(PrereservationBooking.class);
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		Performance performance = mock(Performance.class);
		ScheduleSeat scheduleSeat = mock(ScheduleSeat.class);
		SeatGrade seatGrade = mock(SeatGrade.class);

		given(booking.getMemberId()).willReturn(MEMBER_ID);
		given(booking.getStatus()).willReturn(PrereservationBookingStatus.CREATED);
		given(booking.getScheduleSeatId()).willReturn(SCHEDULE_SEAT_ID);
		given(prereservationBookingService.getBookingOrThrow(BOOKING_ID)).willReturn(booking);

		given(scheduleSeatRepository.findById(SCHEDULE_SEAT_ID)).willReturn(Optional.of(scheduleSeat));
		given(scheduleSeat.getScheduleId()).willReturn(SCHEDULE_ID);
		given(scheduleSeat.getSeatId()).willReturn(SEAT_ID);
		given(scheduleSeat.getStatus()).willReturn(SeatStatus.HOLD);

		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
		given(schedule.getPerformance()).willReturn(performance);
		given(performance.getPerformanceId()).willReturn(PERFORMANCE_ID);

		given(seatGradeRepository.findTopByPerformanceIdAndSeatIdOrderByIdDesc(PERFORMANCE_ID, SEAT_ID))
			.willReturn(Optional.of(seatGrade));
		given(seatGrade.getPrice()).willReturn(50000);

		willDoNothing().given(seatHoldTokenService).validateOwnership(SCHEDULE_ID, SEAT_ID, MEMBER_ID);

		PaymentTarget result = prereservationPaymentHandler.loadAndValidate(BOOKING_ID, MEMBER_ID);

		assertThat(result).isNotNull();
		assertThat(result.domainType()).isEqualTo(DomainType.PRERESERVATION);
		assertThat(result.domainId()).isEqualTo(BOOKING_ID);
		assertThat(result.expectedAmount()).isEqualTo(50000);

		then(seatHoldTokenService).should().validateOwnership(SCHEDULE_ID, SEAT_ID, MEMBER_ID);
	}
}
