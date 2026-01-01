package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.back.b2st.domain.payment.dto.request.PaymentConfirmReq;
import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentMethod;
import com.back.b2st.domain.payment.entity.PaymentStatus;
import com.back.b2st.domain.payment.repository.PaymentRepository;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationSeat;
import com.back.b2st.domain.reservation.entity.ReservationStatus;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.reservation.repository.ReservationSeatRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.ticket.repository.TicketRepository;

@SpringBootTest
@ActiveProfiles("test")
class PaymentReservationFinalizeIntegrationTest {

	@Autowired
	private PaymentConfirmService paymentConfirmService;

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

	@BeforeEach
	void setup() {
		ticketRepository.deleteAll();
		paymentRepository.deleteAll();
		reservationRepository.deleteAll();
		scheduleSeatRepository.deleteAll();
	}

	@Test
	@DisplayName("confirm 성공 시 예매 확정 + 좌석 SOLD + 티켓 발급")
	void confirm_finalizesReservationAndIssuesTicket() {
		// given
		Long memberId = 1L;
		Long scheduleId = 1L;
		Long seatId = 1L;
		Long amount = 15000L;
		String orderId = "ORDER-FINALIZE-1";

		ScheduleSeat scheduleSeat = ScheduleSeat.builder()
			.scheduleId(scheduleId)
			.seatId(seatId)
			.build();
		scheduleSeat.hold(LocalDateTime.now().plusMinutes(5));
		scheduleSeatRepository.save(scheduleSeat);

		Reservation reservation = Reservation.builder()
			.scheduleId(scheduleId)
			.memberId(memberId)
			.expiresAt(LocalDateTime.now().plusMinutes(5))
			.build();
		Reservation savedReservation = reservationRepository.save(reservation);

		ReservationSeat reservationSeat = ReservationSeat.builder()
			.reservationId(reservation.getId())
			.scheduleSeatId(scheduleSeat.getId())
			.build();
		reservationSeatRepository.save(reservationSeat);

		Payment payment = Payment.builder()
			.orderId(orderId)
			.memberId(memberId)
			.domainType(DomainType.RESERVATION)
			.domainId(savedReservation.getId())
			.amount(amount)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		paymentRepository.save(payment);

		// when
		Payment confirmed = paymentConfirmService.confirm(memberId, new PaymentConfirmReq(orderId, amount));

		// then
		assertThat(confirmed.getStatus()).isEqualTo(PaymentStatus.DONE);

		Reservation finalizedReservation = reservationRepository.findById(savedReservation.getId()).orElseThrow();
		assertThat(finalizedReservation.getStatus()).isEqualTo(ReservationStatus.COMPLETED);

		ScheduleSeat finalizedSeat = scheduleSeatRepository.findByScheduleIdAndSeatId(scheduleId, seatId).orElseThrow();
		assertThat(finalizedSeat.getStatus()).isEqualTo(SeatStatus.SOLD);

		assertThat(ticketRepository.findAll()).hasSize(1);
	}
}
