package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.performanceschedule.entity.BookingType;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationStatus;
import com.back.b2st.domain.reservation.error.ReservationErrorCode;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.service.TicketService;
import com.back.b2st.global.error.exception.BusinessException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;

@ExtendWith(MockitoExtension.class)
class PrereservationPaymentFinalizerTest {

	@Mock
	private EntityManager entityManager;

	@Mock
	private PerformanceScheduleRepository performanceScheduleRepository;

	@Mock
	private TicketService ticketService;

	@Mock
	private Clock clock;

	@InjectMocks
	private PrereservationPaymentFinalizer prereservationPaymentFinalizer;

	private static final Long RESERVATION_ID = 1L;
	private static final Long MEMBER_ID = 10L;
	private static final Long SCHEDULE_ID = 100L;
	private static final Long SEAT_ID = 1000L;

	@Test
	@DisplayName("supports(): DomainType.PRERESERVATION 지원")
	void supports_prereservation_true() {
		// when & then
		assertThat(prereservationPaymentFinalizer.supports(DomainType.PRERESERVATION)).isTrue();
	}

	@Test
	@DisplayName("supports(): 다른 도메인 타입은 미지원")
	void supports_others_false() {
		// when & then
		assertThat(prereservationPaymentFinalizer.supports(DomainType.RESERVATION)).isFalse();
		assertThat(prereservationPaymentFinalizer.supports(DomainType.LOTTERY)).isFalse();
		assertThat(prereservationPaymentFinalizer.supports(DomainType.TRADE)).isFalse();
	}

	@Test
	@DisplayName("finalizePayment(): 예약이 없으면 RESERVATION_NOT_FOUND 예외")
	void finalizePayment_reservationNotFound_throw() {
		// given
		Payment payment = mock(Payment.class);
		given(payment.getDomainId()).willReturn(RESERVATION_ID);

		given(entityManager.find(Reservation.class, RESERVATION_ID, LockModeType.PESSIMISTIC_WRITE))
			.willReturn(null);

		// when & then
		assertThatThrownBy(() -> prereservationPaymentFinalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException)ex).getErrorCode())
				.isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND));
	}

	@Test
	@DisplayName("finalizePayment(): 다른 사용자의 예약이면 UNAUTHORIZED_PAYMENT_ACCESS 예외")
	void finalizePayment_unauthorizedMember_throw() {
		// given
		Payment payment = mock(Payment.class);
		Reservation reservation = mock(Reservation.class);

		given(payment.getDomainId()).willReturn(RESERVATION_ID);
		given(payment.getMemberId()).willReturn(MEMBER_ID);
		given(reservation.getMemberId()).willReturn(999L);

		given(entityManager.find(Reservation.class, RESERVATION_ID, LockModeType.PESSIMISTIC_WRITE))
			.willReturn(reservation);

		// when & then
		assertThatThrownBy(() -> prereservationPaymentFinalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException)ex).getErrorCode())
				.isEqualTo(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS));
	}

	@Test
	@DisplayName("finalizePayment(): 취소된 예약이면 RESERVATION_ALREADY_CANCELED 예외")
	void finalizePayment_reservationCanceled_throw() {
		// given
		Payment payment = mock(Payment.class);
		Reservation reservation = mock(Reservation.class);

		given(payment.getDomainId()).willReturn(RESERVATION_ID);
		given(payment.getMemberId()).willReturn(MEMBER_ID);
		given(reservation.getMemberId()).willReturn(MEMBER_ID);
		given(reservation.getStatus()).willReturn(ReservationStatus.CANCELED);

		given(entityManager.find(Reservation.class, RESERVATION_ID, LockModeType.PESSIMISTIC_WRITE))
			.willReturn(reservation);

		// when & then
		assertThatThrownBy(() -> prereservationPaymentFinalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException)ex).getErrorCode())
				.isEqualTo(ReservationErrorCode.RESERVATION_ALREADY_CANCELED));
	}

	@Test
	@DisplayName("finalizePayment(): BookingType이 PRERESERVE가 아니면 예외")
	void finalizePayment_wrongBookingType_throw() {
		// given
		Payment payment = mock(Payment.class);
		Reservation reservation = mock(Reservation.class);
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);

		given(payment.getDomainId()).willReturn(RESERVATION_ID);
		given(payment.getMemberId()).willReturn(MEMBER_ID);
		given(reservation.getMemberId()).willReturn(MEMBER_ID);
		given(reservation.getStatus()).willReturn(ReservationStatus.CREATED);
		given(reservation.getScheduleId()).willReturn(SCHEDULE_ID);

		given(schedule.getBookingType()).willReturn(BookingType.FIRST_COME);

		given(entityManager.find(Reservation.class, RESERVATION_ID, LockModeType.PESSIMISTIC_WRITE))
			.willReturn(reservation);
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		// when & then
		assertThatThrownBy(() -> prereservationPaymentFinalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> {
				assertThat(((BusinessException)ex).getErrorCode()).isEqualTo(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
				assertThat(ex).hasMessageContaining("신청 예매 결제 대상이 아닙니다");
			});
	}

	@Test
	@DisplayName("finalizePayment(): 이미 완료된 예약은 idempotent 처리")
	void finalizePayment_alreadyCompleted_idempotent() {
		// given
		Payment payment = mock(Payment.class);
		Reservation reservation = mock(Reservation.class);
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		ScheduleSeat scheduleSeat = mock(ScheduleSeat.class);
		TypedQuery<ScheduleSeat> query = mock(TypedQuery.class);

		given(payment.getDomainId()).willReturn(RESERVATION_ID);
		given(payment.getMemberId()).willReturn(MEMBER_ID);
		given(reservation.getId()).willReturn(RESERVATION_ID);
		given(reservation.getMemberId()).willReturn(MEMBER_ID);
		given(reservation.getStatus()).willReturn(ReservationStatus.COMPLETED);
		given(reservation.getScheduleId()).willReturn(SCHEDULE_ID);
		given(reservation.getSeatId()).willReturn(SEAT_ID);

		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
		given(scheduleSeat.getStatus()).willReturn(SeatStatus.SOLD);

		given(entityManager.find(Reservation.class, RESERVATION_ID, LockModeType.PESSIMISTIC_WRITE))
			.willReturn(reservation);
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		given(entityManager.createQuery(anyString(), eq(ScheduleSeat.class))).willReturn(query);
		given(query.setParameter("scheduleId", SCHEDULE_ID)).willReturn(query);
		given(query.setParameter("seatId", SEAT_ID)).willReturn(query);
		given(query.setLockMode(LockModeType.PESSIMISTIC_WRITE)).willReturn(query);
		given(query.getResultStream()).willReturn(java.util.stream.Stream.of(scheduleSeat));

		given(ticketService.createTicket(RESERVATION_ID, MEMBER_ID, SEAT_ID)).willReturn(mock(Ticket.class));

		// when
		assertThatCode(() -> prereservationPaymentFinalizer.finalizePayment(payment))
			.doesNotThrowAnyException();

		// then
		then(reservation).should(never()).complete(any());
		then(scheduleSeat).should(never()).sold();
	}

	@Test
	@DisplayName("finalizePayment(): 정상 케이스 - 예약 완료 및 티켓 생성")
	void finalizePayment_success() {
		// given
		Payment payment = mock(Payment.class);
		Reservation reservation = mock(Reservation.class);
		PerformanceSchedule schedule = mock(PerformanceSchedule.class);
		ScheduleSeat scheduleSeat = mock(ScheduleSeat.class);
		TypedQuery<ScheduleSeat> query = mock(TypedQuery.class);

		given(payment.getDomainId()).willReturn(RESERVATION_ID);
		given(payment.getMemberId()).willReturn(MEMBER_ID);
		given(reservation.getId()).willReturn(RESERVATION_ID);
		given(reservation.getMemberId()).willReturn(MEMBER_ID);
		given(reservation.getStatus()).willReturn(ReservationStatus.CREATED);
		given(reservation.getScheduleId()).willReturn(SCHEDULE_ID);
		given(reservation.getSeatId()).willReturn(SEAT_ID);

		given(schedule.getBookingType()).willReturn(BookingType.PRERESERVE);
		given(scheduleSeat.getStatus()).willReturn(SeatStatus.HOLD);

		given(entityManager.find(Reservation.class, RESERVATION_ID, LockModeType.PESSIMISTIC_WRITE))
			.willReturn(reservation);
		given(performanceScheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

		given(entityManager.createQuery(anyString(), eq(ScheduleSeat.class))).willReturn(query);
		given(query.setParameter("scheduleId", SCHEDULE_ID)).willReturn(query);
		given(query.setParameter("seatId", SEAT_ID)).willReturn(query);
		given(query.setLockMode(LockModeType.PESSIMISTIC_WRITE)).willReturn(query);
		given(query.getResultStream()).willReturn(java.util.stream.Stream.of(scheduleSeat));

		Clock fixedClock = Clock.fixed(
			Instant.parse("2025-01-01T14:00:00Z"),
			ZoneId.of("UTC")
		);
		given(clock.instant()).willReturn(fixedClock.instant());
		given(clock.getZone()).willReturn(fixedClock.getZone());

		willDoNothing().given(reservation).complete(any());
		willDoNothing().given(scheduleSeat).sold();
		given(ticketService.createTicket(RESERVATION_ID, MEMBER_ID, SEAT_ID)).willReturn(mock(Ticket.class));

		// when
		assertThatCode(() -> prereservationPaymentFinalizer.finalizePayment(payment))
			.doesNotThrowAnyException();

		// then
		then(reservation).should().complete(any(LocalDateTime.class));
		then(scheduleSeat).should().sold();
		then(ticketService).should().createTicket(RESERVATION_ID, MEMBER_ID, SEAT_ID);
	}
}
