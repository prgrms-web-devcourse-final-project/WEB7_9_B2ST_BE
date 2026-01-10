package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
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
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationSeat;
import com.back.b2st.domain.reservation.entity.ReservationStatus;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.reservation.repository.ReservationSeatRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.scheduleseat.service.SeatHoldTokenService;
import com.back.b2st.domain.seat.grade.entity.SeatGrade;
import com.back.b2st.domain.seat.grade.repository.SeatGradeRepository;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class ReservationPaymentHandlerTest {

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private ReservationSeatRepository reservationSeatRepository;

	@Mock
	private ScheduleSeatRepository scheduleSeatRepository;

	@Mock
	private SeatHoldTokenService seatHoldTokenService;

	@Mock
	private PerformanceScheduleRepository performanceScheduleRepository;

	@Mock
	private SeatGradeRepository seatGradeRepository;

	@InjectMocks
	private ReservationPaymentHandler handler;

	@Test
	@DisplayName("supports: RESERVATION 타입을 지원한다")
	void supports_returnsTrue_forReservation() {
		assertThat(handler.supports(DomainType.RESERVATION)).isTrue();
	}

	@Test
	@DisplayName("loadAndValidate: 정상 케이스 - PaymentTarget 반환")
	void loadAndValidate_success() {
		// Given
		Long reservationId = 1L;
		Long memberId = 100L;
		Long scheduleId = 10L;
		Long seatId = 20L;
		Long scheduleSeatId = 99L;
		Long performanceId = 5L;
		Long expectedPrice = 50000L;

		Reservation reservation =
			createReservation(reservationId, memberId, scheduleId, ReservationStatus.PENDING);

		ReservationSeat reservationSeat = mock(ReservationSeat.class);
		when(reservationSeat.getScheduleSeatId()).thenReturn(scheduleSeatId);

		ScheduleSeat scheduleSeat = mock(ScheduleSeat.class);
		when(scheduleSeat.getStatus()).thenReturn(SeatStatus.HOLD);
		when(scheduleSeat.getScheduleId()).thenReturn(scheduleId);
		when(scheduleSeat.getSeatId()).thenReturn(seatId);

		PerformanceSchedule schedule = createPerformanceSchedule(scheduleId, performanceId);

		SeatGrade seatGrade = createSeatGrade(performanceId, seatId, expectedPrice);

		when(reservationRepository.findById(reservationId))
			.thenReturn(Optional.of(reservation));
		when(reservationSeatRepository.findByReservationId(reservationId))
			.thenReturn(List.of(reservationSeat));
		when(scheduleSeatRepository.findById(scheduleSeatId))
			.thenReturn(Optional.of(scheduleSeat));
		doNothing().when(seatHoldTokenService)
			.validateOwnership(scheduleId, seatId, memberId);
		when(performanceScheduleRepository.findById(scheduleId))
			.thenReturn(Optional.of(schedule));
		when(seatGradeRepository.findTopByPerformanceIdAndSeatIdOrderByIdDesc(performanceId, seatId))
			.thenReturn(Optional.of(seatGrade));

		// When
		PaymentTarget target = handler.loadAndValidate(reservationId, memberId);

		// Then
		assertThat(target.domainType()).isEqualTo(DomainType.RESERVATION);
		assertThat(target.domainId()).isEqualTo(reservationId);
		assertThat(target.expectedAmount()).isEqualTo(expectedPrice);
	}

	@Test
	@DisplayName("loadAndValidate: 예매를 찾을 수 없는 경우 DOMAIN_NOT_FOUND 예외")
	void loadAndValidate_throwsException_whenReservationNotFound() {
		// Given
		when(reservationRepository.findById(1L)).thenReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> handler.loadAndValidate(1L, 100L))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_FOUND);
	}

	@Test
	@DisplayName("loadAndValidate: 다른 회원의 예매인 경우 UNAUTHORIZED_PAYMENT_ACCESS 예외")
	void loadAndValidate_throwsException_whenUnauthorized() {
		// Given
		Reservation reservation = createReservation(1L, 100L, 10L, ReservationStatus.PENDING);
		when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

		// When & Then
		assertThatThrownBy(() -> handler.loadAndValidate(1L, 999L))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
	}

	@Test
	@DisplayName("loadAndValidate: 예매 상태가 PENDING이 아닌 경우 DOMAIN_NOT_PAYABLE 예외")
	void loadAndValidate_throwsException_whenReservationNotPending() {
		// Given
		Reservation reservation = createReservation(1L, 100L, 10L, ReservationStatus.COMPLETED);
		when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

		// When & Then
		assertThatThrownBy(() -> handler.loadAndValidate(1L, 100L))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
	}

	@Test
	@DisplayName("loadAndValidate: 스케줄 좌석을 찾을 수 없는 경우 DOMAIN_NOT_FOUND 예외")
	void loadAndValidate_throwsException_whenScheduleSeatNotFound() {
		// Given
		Reservation reservation = createReservation(1L, 100L, 10L, ReservationStatus.PENDING);
		ReservationSeat reservationSeat = mock(ReservationSeat.class);

		when(reservationSeat.getScheduleSeatId()).thenReturn(999L);
		when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
		when(reservationSeatRepository.findByReservationId(1L)).thenReturn(List.of(reservationSeat));
		when(scheduleSeatRepository.findById(999L)).thenReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> handler.loadAndValidate(1L, 100L))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_FOUND);
	}

	@Test
	@DisplayName("loadAndValidate: 좌석 상태가 HOLD가 아닌 경우 DOMAIN_NOT_PAYABLE 예외")
	void loadAndValidate_throwsException_whenSeatNotHold() {
		// Given
		Reservation reservation = createReservation(1L, 100L, 10L, ReservationStatus.PENDING);

		ReservationSeat reservationSeat = mock(ReservationSeat.class);
		when(reservationSeat.getScheduleSeatId()).thenReturn(20L);

		ScheduleSeat scheduleSeat = createScheduleSeat(10L, 20L, SeatStatus.AVAILABLE);

		when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
		when(reservationSeatRepository.findByReservationId(1L)).thenReturn(List.of(reservationSeat));
		when(scheduleSeatRepository.findById(20L)).thenReturn(Optional.of(scheduleSeat));

		// When & Then
		assertThatThrownBy(() -> handler.loadAndValidate(1L, 100L))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
	}

	@Test
	@DisplayName("loadAndValidate: 공연 일정을 찾을 수 없는 경우 DOMAIN_NOT_FOUND 예외")
	void loadAndValidate_throwsException_whenScheduleNotFound() {
		// Given
		Reservation reservation = createReservation(1L, 100L, 10L, ReservationStatus.PENDING);
		ReservationSeat reservationSeat = mock(ReservationSeat.class);
		when(reservationSeat.getScheduleSeatId()).thenReturn(20L);

		ScheduleSeat scheduleSeat = createScheduleSeat(10L, 20L, SeatStatus.HOLD);
		when(scheduleSeat.getStatus()).thenReturn(SeatStatus.HOLD);
		when(scheduleSeat.getScheduleId()).thenReturn(10L);
		when(scheduleSeat.getSeatId()).thenReturn(20L);

		when(reservationRepository.findById(1L))
			.thenReturn(Optional.of(reservation));
		when(reservationSeatRepository.findByReservationId(1L))
			.thenReturn(List.of(reservationSeat));
		when(scheduleSeatRepository.findById(20L))
			.thenReturn(Optional.of(scheduleSeat));

		doNothing().when(seatHoldTokenService).validateOwnership(10L, 20L, 100L);

		when(performanceScheduleRepository.findById(10L))
			.thenReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> handler.loadAndValidate(1L, 100L))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_FOUND);
	}

	@Test
	@DisplayName("loadAndValidate: 좌석 등급을 찾을 수 없는 경우 DOMAIN_NOT_PAYABLE 예외")
	void loadAndValidate_throwsException_whenSeatGradeNotFound() {
		// Given
		Long reservationId = 1L;
		Long memberId = 100L;
		Long scheduleId = 10L;
		Long seatId = 20L;
		Long performanceId = 5L;

		Reservation reservation = createReservation(reservationId, memberId, scheduleId, ReservationStatus.PENDING);

		ReservationSeat reservationSeat = mock(ReservationSeat.class);
		when(reservationSeat.getScheduleSeatId()).thenReturn(20L);

		ScheduleSeat scheduleSeat = createScheduleSeat(scheduleId, seatId, SeatStatus.HOLD);
		when(scheduleSeat.getStatus()).thenReturn(SeatStatus.HOLD);
		when(scheduleSeat.getScheduleId()).thenReturn(scheduleId);
		when(scheduleSeat.getSeatId()).thenReturn(seatId);

		PerformanceSchedule schedule = createPerformanceSchedule(scheduleId, performanceId);

		when(reservationRepository.findById(reservationId))
			.thenReturn(Optional.of(reservation));
		when(reservationSeatRepository.findByReservationId(reservationId))
			.thenReturn(List.of(reservationSeat));
		when(scheduleSeatRepository.findById(20L))
			.thenReturn(Optional.of(scheduleSeat));

		doNothing().when(seatHoldTokenService).validateOwnership(scheduleId, seatId, memberId);

		when(performanceScheduleRepository.findById(scheduleId))
			.thenReturn(Optional.of(schedule));
		when(seatGradeRepository
			.findTopByPerformanceIdAndSeatIdOrderByIdDesc(performanceId, seatId))
			.thenReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> handler.loadAndValidate(reservationId, memberId))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
	}

	private Reservation createReservation(Long id, Long memberId, Long scheduleId, ReservationStatus status) {
		Reservation reservation = Reservation.builder()
			.memberId(memberId)
			.scheduleId(scheduleId)
			.expiresAt(LocalDateTime.now().plusMinutes(5))
			.build();
		setField(reservation, "id", id);
		setField(reservation, "status", status);
		return reservation;
	}

	private void setField(Object target, String fieldName, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private ScheduleSeat createScheduleSeat(Long scheduleId, Long seatId, SeatStatus status) {
		ScheduleSeat scheduleSeat = mock(ScheduleSeat.class);
		when(scheduleSeat.getStatus()).thenReturn(status);
		return scheduleSeat;
	}

	private PerformanceSchedule createPerformanceSchedule(Long scheduleId, Long performanceId) {
		Performance performance = mock(Performance.class);
		when(performance.getPerformanceId()).thenReturn(performanceId);

		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		when(schedule.getPerformance()).thenReturn(performance);
		return schedule;
	}

	private SeatGrade createSeatGrade(Long performanceId, Long seatId, Long price) {
		SeatGrade seatGrade = mock(SeatGrade.class);
		when(seatGrade.getPrice()).thenReturn(price.intValue());
		return seatGrade;
	}

	private void createReservationSeat(Reservation reservation, ScheduleSeat seat) {
		reservationSeatRepository.save(
			ReservationSeat.builder()
				.reservationId(reservation.getId())
				.scheduleSeatId(seat.getId())
				.build()
		);
	}
}
