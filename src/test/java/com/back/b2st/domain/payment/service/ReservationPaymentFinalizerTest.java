package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.*;

import java.time.Clock;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentMethod;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.payment.repository.PaymentRepository;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationSeat;
import com.back.b2st.domain.reservation.entity.ReservationStatus;
import com.back.b2st.domain.reservation.error.ReservationErrorCode;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.reservation.repository.ReservationSeatRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.repository.TicketRepository;
import com.back.b2st.global.error.exception.BusinessException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReservationPaymentFinalizerTest {

	@Autowired
	private ReservationPaymentFinalizer finalizer;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private ReservationSeatRepository reservationSeatRepository;

	@Autowired
	private ScheduleSeatRepository scheduleSeatRepository;

	@Autowired
	private TicketRepository ticketRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private Clock clock;

	private Long memberId;
	private Long scheduleId;
	private Long seatId;

	@BeforeEach
	void setup() {
		ticketRepository.deleteAll();
		reservationRepository.deleteAll();
		scheduleSeatRepository.deleteAll();
		paymentRepository.deleteAll();

		memberId = 1L;
		scheduleId = 100L;
		seatId = 200L;
	}

	@Test
	@DisplayName("결제 확정 시 예매 확정 + 좌석 판매 + 티켓 발급")
	void finalizePayment_success() {
		// given
		ScheduleSeat scheduleSeat = createScheduleSeat(SeatStatus.HOLD);
		Reservation reservation = createReservation(ReservationStatus.PENDING);
		createReservationSeat(reservation, scheduleSeat);
		Payment payment = createDonePayment(reservation.getId());

		// when
		finalizer.finalizePayment(payment);
		entityManager.flush();
		entityManager.clear();

		// then
		Reservation updatedReservation = reservationRepository.findById(reservation.getId()).orElseThrow();
		assertThat(updatedReservation.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
		assertThat(updatedReservation.getCompletedAt()).isNotNull();

		ScheduleSeat updatedSeat = scheduleSeatRepository.findById(scheduleSeat.getId()).orElseThrow();
		assertThat(updatedSeat.getStatus()).isEqualTo(SeatStatus.SOLD);

		Ticket ticket = ticketRepository.findByReservationIdAndMemberIdAndSeatId(
			reservation.getId(), memberId, seatId).orElseThrow();
		assertThat(ticket).isNotNull();
		assertThat(ticket.getReservationId()).isEqualTo(reservation.getId());
	}

	@Test
	@DisplayName("멱등: 이미 확정된 예매는 다시 처리해도 안전")
	void finalizePayment_idempotent_whenAlreadyCompleted() {
		// given
		ScheduleSeat scheduleSeat = createScheduleSeat(SeatStatus.SOLD);
		Reservation reservation = createReservation(ReservationStatus.COMPLETED);
		createReservationSeat(reservation, scheduleSeat);
		reservation.complete(LocalDateTime.now(clock));
		reservationRepository.save(reservation);

		Payment payment = createDonePayment(reservation.getId());

		// 티켓도 이미 존재
		ticketRepository.save(Ticket.builder()
			.reservationId(reservation.getId())
			.memberId(memberId)
			.seatId(seatId)
			.build());

		entityManager.flush();
		entityManager.clear();

		long ticketCountBefore = ticketRepository.count();

		// when
		finalizer.finalizePayment(payment);
		entityManager.flush();

		// then - 중복 생성 없이 정상 처리
		long ticketCountAfter = ticketRepository.count();
		assertThat(ticketCountAfter).isEqualTo(ticketCountBefore);

		Reservation updatedReservation = reservationRepository.findById(reservation.getId()).orElseThrow();
		assertThat(updatedReservation.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
	}

	@Test
	@DisplayName("예매가 존재하지 않으면 예외 발생")
	void finalizePayment_throwsNotFound_whenReservationNotExists() {
		// given
		Payment payment = createDonePayment(999L);

		// when & then
		assertThatThrownBy(() -> finalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND);
	}

	@Test
	@DisplayName("예매 소유자와 결제 소유자가 다르면 예외 발생")
	void finalizePayment_throwsUnauthorized_whenMemberMismatch() {
		// given
		Reservation reservation = createReservation(ReservationStatus.PENDING);
		Payment payment = Payment.builder()
			.orderId("order-123")
			.memberId(999L) // 다른 memberId
			.domainType(DomainType.RESERVATION)
			.domainId(reservation.getId())
			.amount(15000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete(LocalDateTime.now());
		paymentRepository.save(payment);

		// when & then
		assertThatThrownBy(() -> finalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
	}

	@Test
	@DisplayName("취소된 예매는 확정 불가")
	void finalizePayment_throwsError_whenReservationCanceled() {
		// given
		createScheduleSeat(SeatStatus.AVAILABLE);
		Reservation reservation = createReservation(ReservationStatus.CANCELED);
		Payment payment = createDonePayment(reservation.getId());

		// when & then
		assertThatThrownBy(() -> finalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(ReservationErrorCode.RESERVATION_ALREADY_CANCELED);
	}

	@Test
	@DisplayName("좌석이 HOLD가 아니면 예외 발생")
	void finalizePayment_throwsError_whenSeatNotHold() {
		// given
		ScheduleSeat seat = createScheduleSeat(SeatStatus.AVAILABLE);// HOLD가 아닌 AVAILABLE
		Reservation reservation = createReservation(ReservationStatus.PENDING);
		createReservationSeat(reservation, seat);
		Payment payment = createDonePayment(reservation.getId());

		// when & then
		assertThatThrownBy(() -> finalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
	}

	@Test
	@DisplayName("좌석이 존재하지 않으면 예외 발생")
	void finalizePayment_throwsError_whenSeatNotExists() {
		// given
		Reservation reservation = createReservation(ReservationStatus.PENDING);
		Payment payment = createDonePayment(reservation.getId());
		// scheduleSeat 생성 안함

		// when & then
		assertThatThrownBy(() -> finalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_FOUND);
	}

	private ScheduleSeat createScheduleSeat(SeatStatus status) {
		ScheduleSeat seat = ScheduleSeat.builder()
			.scheduleId(scheduleId)
			.seatId(seatId)
			.build();

		// 상태 전환
		if (status == SeatStatus.HOLD) {
			seat.hold(LocalDateTime.now().plusMinutes(5));
		} else if (status == SeatStatus.SOLD) {
			seat.sold();
		}

		return scheduleSeatRepository.save(seat);
	}

	private Reservation createReservation(ReservationStatus status) {
		Reservation reservation = Reservation.builder()
			.memberId(memberId)
			.scheduleId(scheduleId)
			.expiresAt(LocalDateTime.now().plusMinutes(5))
			.build();

		// 상태 전환
		if (status == ReservationStatus.CANCELED) {
			reservation.cancel(LocalDateTime.now());
		} else if (status == ReservationStatus.COMPLETED) {
			reservation.complete(LocalDateTime.now(clock));
		}

		return reservationRepository.save(reservation);
	}

	private Payment createDonePayment(Long reservationId) {
		Payment payment = Payment.builder()
			.orderId("order-" + System.nanoTime())
			.memberId(memberId)
			.domainType(DomainType.RESERVATION)
			.domainId(reservationId)
			.amount(15000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete(LocalDateTime.now());
		return paymentRepository.save(payment);
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
