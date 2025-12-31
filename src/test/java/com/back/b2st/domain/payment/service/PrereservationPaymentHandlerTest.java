package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.math.BigDecimal;
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
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationStatus;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
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
	private ReservationRepository reservationRepository;

	@Mock
	private ScheduleSeatRepository scheduleSeatRepository;

	@Mock
	private SeatHoldTokenService seatHoldTokenService;

	@Mock
	private PerformanceScheduleRepository performanceScheduleRepository;

	@Mock
	private SeatGradeRepository seatGradeRepository;

	@InjectMocks
	private PrereservationPaymentHandler prereservationPaymentHandler;

	private static final Long RESERVATION_ID = 1L;
	private static final Long MEMBER_ID = 10L;
	private static final Long SCHEDULE_ID = 100L;
	private static final Long SEAT_ID = 1000L;
	private static final Long PERFORMANCE_ID = 50L;

	@Test
	@DisplayName("supports(): DomainType.PRERESERVATION 지원")
	void supports_prereservation_true() {
		// when & then
		assertThat(prereservationPaymentHandler.supports(DomainType.PRERESERVATION)).isTrue();
	}

	@Test
	@DisplayName("supports(): 다른 도메인 타입은 미지원")
	void supports_others_false() {
		// when & then
		assertThat(prereservationPaymentHandler.supports(DomainType.RESERVATION)).isFalse();
		assertThat(prereservationPaymentHandler.supports(DomainType.LOTTERY)).isFalse();
		assertThat(prereservationPaymentHandler.supports(DomainType.TRADE)).isFalse();
	}

	@Test
	@DisplayName("loadAndValidate(): 예약이 없으면 DOMAIN_NOT_FOUND 예외")
	void loadAndValidate_reservationNotFound_throw() {
		// given
		given(reservationRepository.findById(RESERVATION_ID)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(
			() -> prereservationPaymentHandler.loadAndValidate(RESERVATION_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PaymentErrorCode.DOMAIN_NOT_FOUND.getMessage());
	}

	@Test
	@DisplayName("loadAndValidate(): 다른 사용자의 예약이면 UNAUTHORIZED_PAYMENT_ACCESS 예외")
	void loadAndValidate_unauthorizedMember_throw() {
		// given
		Reservation reservation = mock(Reservation.class);
		given(reservation.getMemberId()).willReturn(999L);

		given(reservationRepository.findById(RESERVATION_ID)).willReturn(Optional.of(reservation));

		// when & then
		assertThatThrownBy(
			() -> prereservationPaymentHandler.loadAndValidate(RESERVATION_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS.getMessage());
	}

	@Test
	@DisplayName("loadAndValidate(): 결제 불가능한 상태면 DOMAIN_NOT_PAYABLE 예외")
	void loadAndValidate_notPayableStatus_throw() {
		// given
		Reservation reservation = mock(Reservation.class);
		given(reservation.getMemberId()).willReturn(MEMBER_ID);
		given(reservation.getStatus()).willReturn(ReservationStatus.COMPLETED);

		given(reservationRepository.findById(RESERVATION_ID)).willReturn(Optional.of(reservation));

		// when & then
		assertThatThrownBy(
			() -> prereservationPaymentHandler.loadAndValidate(RESERVATION_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PaymentErrorCode.DOMAIN_NOT_PAYABLE.getMessage());
	}

	@Test
	@DisplayName("loadAndValidate(): 스케줄이 없으면 DOMAIN_NOT_FOUND 예외")
	void loadAndValidate_scheduleNotFound_throw() {
		// given
		Reservation reservation = mock(Reservation.class);
		given(reservation.getMemberId()).willReturn(MEMBER_ID);
		given(reservation.getStatus()).willReturn(ReservationStatus.CREATED);
		given(reservation.getScheduleId()).willReturn(SCHEDULE_ID);

		given(reservationRepository.findById(RESERVATION_ID)).willReturn(Optional.of(reservation));
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(
			() -> prereservationPaymentHandler.loadAndValidate(RESERVATION_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PaymentErrorCode.DOMAIN_NOT_FOUND.getMessage());
	}

	@Test
	@DisplayName("loadAndValidate(): BookingType이 PRERESERVE가 아니면 DOMAIN_NOT_PAYABLE 예외")
	void loadAndValidate_wrongBookingType_throw() {
		// given
		Reservation reservation = mock(Reservation.class);
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);

		given(reservation.getMemberId()).willReturn(MEMBER_ID);
		given(reservation.getStatus()).willReturn(ReservationStatus.CREATED);
		given(reservation.getScheduleId()).willReturn(SCHEDULE_ID);

		given(schedule.getBookingType()).willReturn(BookingType.FIRST_COME);

		given(reservationRepository.findById(RESERVATION_ID)).willReturn(Optional.of(reservation));
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		// when & then
		assertThatThrownBy(
			() -> prereservationPaymentHandler.loadAndValidate(RESERVATION_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("신청 예매 결제 대상이 아닙니다");
	}

	@Test
	@DisplayName("loadAndValidate(): 좌석이 HOLD 상태가 아니면 DOMAIN_NOT_PAYABLE 예외")
	void loadAndValidate_seatNotHold_throw() {
		// given
		Reservation reservation = mock(Reservation.class);
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		ScheduleSeat scheduleSeat = mock(ScheduleSeat.class);

		given(reservation.getMemberId()).willReturn(MEMBER_ID);
		given(reservation.getStatus()).willReturn(ReservationStatus.CREATED);
		given(reservation.getScheduleId()).willReturn(SCHEDULE_ID);
		given(reservation.getSeatId()).willReturn(SEAT_ID);

		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
		given(scheduleSeat.getStatus()).willReturn(SeatStatus.AVAILABLE);

		given(reservationRepository.findById(RESERVATION_ID)).willReturn(Optional.of(reservation));
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(scheduleSeatRepository.findByScheduleIdAndSeatId(SCHEDULE_ID, SEAT_ID))
			.willReturn(Optional.of(scheduleSeat));

		// when & then
		assertThatThrownBy(
			() -> prereservationPaymentHandler.loadAndValidate(RESERVATION_ID, MEMBER_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(PaymentErrorCode.DOMAIN_NOT_PAYABLE.getMessage());
	}

	@Test
	@DisplayName("loadAndValidate(): 정상 케이스 - PaymentTarget 반환")
	void loadAndValidate_success() {
		// given
		Reservation reservation = mock(Reservation.class);
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		Performance performance = mock(Performance.class);
		ScheduleSeat scheduleSeat = mock(ScheduleSeat.class);
		SeatGrade seatGrade = mock(SeatGrade.class);

		given(reservation.getMemberId()).willReturn(MEMBER_ID);
		given(reservation.getStatus()).willReturn(ReservationStatus.CREATED);
		given(reservation.getScheduleId()).willReturn(SCHEDULE_ID);
		given(reservation.getSeatId()).willReturn(SEAT_ID);

		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
		given(schedule.getPerformance()).willReturn(performance);
		given(performance.getPerformanceId()).willReturn(PERFORMANCE_ID);

		given(scheduleSeat.getStatus()).willReturn(SeatStatus.HOLD);
		given(seatGrade.getPrice()).willReturn(BigDecimal.valueOf(50000));

		given(reservationRepository.findById(RESERVATION_ID)).willReturn(Optional.of(reservation));
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
		given(scheduleSeatRepository.findByScheduleIdAndSeatId(SCHEDULE_ID, SEAT_ID))
			.willReturn(Optional.of(scheduleSeat));
		given(seatGradeRepository.findTopByPerformanceIdAndSeatIdOrderByIdDesc(PERFORMANCE_ID, SEAT_ID))
			.willReturn(Optional.of(seatGrade));

		willDoNothing().given(seatHoldTokenService).validateOwnership(SCHEDULE_ID, SEAT_ID, MEMBER_ID);

		// when
		PaymentTarget result = prereservationPaymentHandler.loadAndValidate(RESERVATION_ID, MEMBER_ID);

		// then
		assertThat(result).isNotNull();
		assertThat(result.domainType()).isEqualTo(DomainType.PRERESERVATION);
		assertThat(result.domainId()).isEqualTo(RESERVATION_ID);
		assertThat(result.amount()).isEqualTo(50000L);

		then(seatHoldTokenService).should().validateOwnership(SCHEDULE_ID, SEAT_ID, MEMBER_ID);
	}
}