package com.back.b2st.domain.trade.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.repository.TicketRepository;
import com.back.b2st.domain.trade.dto.request.CreateTradeRequestReq;
import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeRequest;
import com.back.b2st.domain.trade.entity.TradeRequestStatus;
import com.back.b2st.domain.trade.entity.TradeType;
import com.back.b2st.domain.trade.repository.TradeRepository;
import com.back.b2st.domain.trade.repository.TradeRequestRepository;
import com.back.b2st.global.error.exception.BusinessException;
import com.back.b2st.global.test.AbstractContainerBaseTest;

@SpringBootTest
@ActiveProfiles("test")
class TradeRequestConcurrencyTest extends AbstractContainerBaseTest {

	@Autowired
	private TradeRequestService tradeRequestService;

	@Autowired
	private TradeRepository tradeRepository;

	@Autowired
	private TradeRequestRepository tradeRequestRepository;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private SeatRepository seatRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@AfterEach
	void tearDown() {
		tradeRequestRepository.deleteAll();
		tradeRepository.deleteAll();
		ticketRepository.deleteAll();
		reservationRepository.deleteAll();
		seatRepository.deleteAll();
	}

	@Test
	@DisplayName("동시에 여러 교환 신청을 수락해도 하나만 수락되어야 함 (Pessimistic Lock)")
	void acceptTradeRequest_concurrency_onlyOneAccepted() throws InterruptedException {
		// given
		Long ownerId = 100L;
		Long requester1Id = 200L;
		Long requester2Id = 300L;

		// 좌석 생성
		Seat seat1 = Seat.builder()
			.venueId(1L)
			.sectionId(1L)
			.sectionName("A")
			.rowLabel("5열")
			.seatNumber(12)
			.build();
		Seat seat2 = Seat.builder()
			.venueId(1L)
			.sectionId(1L)
			.sectionName("A")
			.rowLabel("6열")
			.seatNumber(13)
			.build();
		Seat seat3 = Seat.builder()
			.venueId(1L)
			.sectionId(1L)
			.sectionName("A")
			.rowLabel("7열")
			.seatNumber(14)
			.build();
		seatRepository.save(seat1);
		seatRepository.save(seat2);
		seatRepository.save(seat3);

		// 예약 생성
		Reservation reservation1 = Reservation.builder()
			.performanceId(1L)
			.memberId(ownerId)
			.seatId(seat1.getId())
			.expiresAt(LocalDateTime.now().plusMinutes(5))
			.build();
		Reservation reservation2 = Reservation.builder()
			.performanceId(1L)
			.memberId(requester1Id)
			.seatId(seat2.getId())
			.expiresAt(LocalDateTime.now().plusMinutes(5))
			.build();
		Reservation reservation3 = Reservation.builder()
			.performanceId(1L)
			.memberId(requester2Id)
			.seatId(seat3.getId())
			.expiresAt(LocalDateTime.now().plusMinutes(5))
			.build();
		reservationRepository.save(reservation1);
		reservationRepository.save(reservation2);
		reservationRepository.save(reservation3);

		// 티켓 생성
		Ticket ownerTicket = Ticket.builder()
			.reservationId(reservation1.getId())
			.memberId(ownerId)
			.seatId(seat1.getId())
			.qrCode("QR1")
			.build();
		Ticket requester1Ticket = Ticket.builder()
			.reservationId(reservation2.getId())
			.memberId(requester1Id)
			.seatId(seat2.getId())
			.qrCode("QR2")
			.build();
		Ticket requester2Ticket = Ticket.builder()
			.reservationId(reservation3.getId())
			.memberId(requester2Id)
			.seatId(seat3.getId())
			.qrCode("QR3")
			.build();
		ticketRepository.save(ownerTicket);
		ticketRepository.save(requester1Ticket);
		ticketRepository.save(requester2Ticket);

		// Trade 생성
		Trade trade = Trade.builder()
			.memberId(ownerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(ownerTicket.getId())
			.type(TradeType.EXCHANGE)
			.price(null)
			.totalCount(1)
			.section("A")
			.row("5열")
			.seatNumber("12")
			.build();
		tradeRepository.save(trade);

		// 두 개의 교환 신청 생성
		CreateTradeRequestReq request1 = new CreateTradeRequestReq(requester1Ticket.getId());
		CreateTradeRequestReq request2 = new CreateTradeRequestReq(requester2Ticket.getId());

		tradeRequestService.createTradeRequest(trade.getId(), request1, requester1Id);
		tradeRequestService.createTradeRequest(trade.getId(), request2, requester2Id);

		var tradeRequests = tradeRequestRepository.findByTrade(trade);
		Long request1Id = tradeRequests.get(0).getId();
		Long request2Id = tradeRequests.get(1).getId();

		// when - 동시에 두 요청을 수락 시도
		int threadCount = 2;
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failCount = new AtomicInteger(0);

		executorService.submit(() -> {
			try {
				tradeRequestService.acceptTradeRequest(request1Id, ownerId);
				successCount.incrementAndGet();
			} catch (BusinessException e) {
				failCount.incrementAndGet();
			} finally {
				latch.countDown();
			}
		});

		executorService.submit(() -> {
			try {
				tradeRequestService.acceptTradeRequest(request2Id, ownerId);
				successCount.incrementAndGet();
			} catch (BusinessException e) {
				failCount.incrementAndGet();
			} finally {
				latch.countDown();
			}
		});

		latch.await();
		executorService.shutdown();

		// then - 하나만 성공해야 함
		assertThat(successCount.get()).isEqualTo(1);
		assertThat(failCount.get()).isEqualTo(1);

		// 실제 DB 상태 확인
		var requests = tradeRequestRepository.findByTrade(trade);
		long acceptedCount = requests.stream()
			.filter(req -> req.getStatus() == TradeRequestStatus.ACCEPTED)
			.count();

		assertThat(acceptedCount).isEqualTo(1);
	}

	@Test
	@DisplayName("Accept와 Reject를 동시에 호출해도 하나만 성공해야 함 (Pessimistic Lock)")
	void acceptAndRejectTradeRequest_concurrency() throws InterruptedException {
		// given
		Long ownerId = 100L;
		Long requesterId = 200L;

		// Seat 생성
		Seat seat1 = Seat.builder()
			.venueId(1L)
			.sectionId(1L)
			.sectionName("A")
			.rowLabel("5열")
			.seatNumber(1)
			.build();
		Seat seat2 = Seat.builder()
			.venueId(1L)
			.sectionId(1L)
			.sectionName("A")
			.rowLabel("5열")
			.seatNumber(2)
			.build();
		seatRepository.save(seat1);
		seatRepository.save(seat2);

		// Reservation 생성
		Reservation reservation1 = Reservation.builder()
			.performanceId(1L)
			.memberId(ownerId)
			.seatId(seat1.getId())
			.expiresAt(LocalDateTime.now().plusMinutes(5))
			.build();
		Reservation reservation2 = Reservation.builder()
			.performanceId(1L)
			.memberId(requesterId)
			.seatId(seat2.getId())
			.expiresAt(LocalDateTime.now().plusMinutes(5))
			.build();
		reservationRepository.save(reservation1);
		reservationRepository.save(reservation2);

		// 티켓 생성
		Ticket ownerTicket = Ticket.builder()
			.reservationId(reservation1.getId())
			.memberId(ownerId)
			.seatId(seat1.getId())
			.qrCode("QR1")
			.build();
		Ticket requesterTicket = Ticket.builder()
			.reservationId(reservation2.getId())
			.memberId(requesterId)
			.seatId(seat2.getId())
			.qrCode("QR2")
			.build();
		ticketRepository.save(ownerTicket);
		ticketRepository.save(requesterTicket);

		// Trade 생성
		Trade trade = Trade.builder()
			.memberId(ownerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(ownerTicket.getId())
			.type(TradeType.EXCHANGE)
			.price(null)
			.totalCount(1)
			.section("A")
			.row("5열")
			.seatNumber("1석")
			.build();
		tradeRepository.save(trade);

		// TradeRequest 생성
		TradeRequest tradeRequest = TradeRequest.builder()
			.trade(trade)
			.requesterId(requesterId)
			.requesterTicketId(requesterTicket.getId())
			.build();
		tradeRequestRepository.save(tradeRequest);

		Long requestId = tradeRequest.getId();

		// when - Accept와 Reject를 동시에 호출
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		CountDownLatch latch = new CountDownLatch(2);
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failCount = new AtomicInteger(0);

		// Accept 스레드
		executorService.submit(() -> {
			try {
				tradeRequestService.acceptTradeRequest(requestId, ownerId);
				successCount.incrementAndGet();
			} catch (BusinessException e) {
				failCount.incrementAndGet();
			} finally {
				latch.countDown();
			}
		});

		// Reject 스레드
		executorService.submit(() -> {
			try {
				tradeRequestService.rejectTradeRequest(requestId, ownerId);
				successCount.incrementAndGet();
			} catch (BusinessException e) {
				failCount.incrementAndGet();
			} finally {
				latch.countDown();
			}
		});

		latch.await();
		executorService.shutdown();

		// then - 하나만 성공해야 함
		assertThat(successCount.get()).isEqualTo(1);
		assertThat(failCount.get()).isEqualTo(1);

		// 실제 DB 상태 확인 - ACCEPTED 또는 REJECTED 중 하나만 있어야 함
		TradeRequest finalRequest = tradeRequestRepository.findById(requestId).orElseThrow();
		assertThat(finalRequest.getStatus()).isIn(TradeRequestStatus.ACCEPTED, TradeRequestStatus.REJECTED);
		assertThat(finalRequest.getStatus()).isNotEqualTo(TradeRequestStatus.PENDING);
	}
}
