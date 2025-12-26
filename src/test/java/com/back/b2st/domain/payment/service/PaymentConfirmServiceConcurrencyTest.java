package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.ticket.repository.TicketRepository;

@SpringBootTest
@ActiveProfiles("test")
class PaymentConfirmServiceConcurrencyTest {

	@Autowired
	private PaymentConfirmService paymentConfirmService;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private ScheduleSeatRepository scheduleSeatRepository;

	@Autowired
	private TicketRepository ticketRepository;

	private Long memberId;

	@BeforeEach
	void setup() {
		ticketRepository.deleteAll();
		paymentRepository.deleteAll();
		reservationRepository.deleteAll();
		scheduleSeatRepository.deleteAll();
		memberId = 1L;
	}

	@Test
	@DisplayName("confirm 동시 호출 - DONE 1번만 반영(멱등)")
	void confirm_concurrent_onlyOneApplied() throws Exception {
		// given
		String orderId = "ORDER-CONC";
		Long amount = 30000L;
		Long scheduleId = 1L;
		Long seatId = 1L;

		ScheduleSeat scheduleSeat = ScheduleSeat.builder()
			.scheduleId(scheduleId)
			.seatId(seatId)
			.build();
		scheduleSeat.hold(LocalDateTime.now().plusMinutes(5));
		scheduleSeatRepository.save(scheduleSeat);

		Reservation reservation = Reservation.builder()
			.scheduleId(scheduleId)
			.memberId(memberId)
			.seatId(seatId)
			.expiresAt(LocalDateTime.now().plusMinutes(5))
			.build();
		Reservation savedReservation = reservationRepository.save(reservation);

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

		int threads = 2;
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		CountDownLatch startLatch = new CountDownLatch(1);

		Callable<Void> task = () -> {
			startLatch.await();
			paymentConfirmService.confirm(memberId, new PaymentConfirmReq(orderId, amount));
			return null;
		};

		List<Future<Void>> futures = new ArrayList<>();
		for (int i = 0; i < threads; i++) {
			futures.add(executor.submit(task));
		}
		startLatch.countDown();

		for (Future<Void> future : futures) {
			future.get();
		}
		executor.shutdown();

		// then
		Payment confirmed = paymentRepository.findByOrderId(orderId).orElseThrow();
		assertThat(confirmed.getStatus()).isEqualTo(PaymentStatus.DONE);
		assertThat(ticketRepository.findAll()).hasSize(1);
	}
}
