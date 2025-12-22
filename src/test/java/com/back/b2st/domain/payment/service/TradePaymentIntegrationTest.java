package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.back.b2st.domain.payment.dto.request.PaymentConfirmReq;
import com.back.b2st.domain.payment.dto.request.PaymentPrepareReq;
import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentMethod;
import com.back.b2st.domain.payment.entity.PaymentStatus;
import com.back.b2st.domain.payment.repository.PaymentRepository;
import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.entity.TicketStatus;
import com.back.b2st.domain.ticket.repository.TicketRepository;
import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.entity.TradeType;
import com.back.b2st.domain.trade.repository.TradeRepository;
import com.back.b2st.global.error.exception.BusinessException;

@SpringBootTest
@ActiveProfiles("test")
class TradePaymentIntegrationTest {

	@Autowired
	private PaymentPrepareService paymentPrepareService;

	@Autowired
	private PaymentConfirmService paymentConfirmService;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private TradeRepository tradeRepository;

	@Autowired
	private TicketRepository ticketRepository;

	@BeforeEach
	void setup() {
		ticketRepository.deleteAll();
		paymentRepository.deleteAll();
		tradeRepository.deleteAll();
	}

	@Test
	@DisplayName("양도 결제 전체 플로우 성공 - 결제 준비 → 승인 → 티켓 양도")
	void tradePayment_fullFlow_success() {
		// given: 판매자의 티켓 생성
		Long sellerId = 1L;
		Long buyerId = 2L;
		Long reservationId = 1L;
		Long seatId = 1L;
		Long tradePrice = 50000L;

		Ticket sellerTicket = Ticket.builder()
			.reservationId(reservationId)
			.memberId(sellerId)
			.seatId(seatId)
			.qrCode("QR-SELLER-1")
			.build();
		ticketRepository.save(sellerTicket);

		// given: 양도글 생성
		Trade trade = Trade.builder()
			.memberId(sellerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(sellerTicket.getId())
			.type(TradeType.TRANSFER)
			.price(tradePrice.intValue())
			.totalCount(1)
			.section("A구역")
			.row("5열")
			.seatNumber("12")
			.build();
		Trade savedTrade = tradeRepository.save(trade);

		// when: 구매자가 결제 준비
		PaymentPrepareReq prepareReq = new PaymentPrepareReq(
			DomainType.TRADE,
			savedTrade.getId(),
			PaymentMethod.CARD
		);
		Payment preparedPayment = paymentPrepareService.prepare(buyerId, prepareReq);

		// then: 결제 준비 완료
		assertThat(preparedPayment.getStatus()).isEqualTo(PaymentStatus.READY);
		assertThat(preparedPayment.getAmount()).isEqualTo(tradePrice);
		assertThat(preparedPayment.getMemberId()).isEqualTo(buyerId);

		// when: 구매자가 결제 승인 (결제 완료 처리는 Mock 없이 직접 수행)
		String orderId = preparedPayment.getOrderId();
		Payment paymentToConfirm = paymentRepository.findByOrderId(orderId).orElseThrow();
		paymentToConfirm.complete("PAYMENT-KEY-123", java.time.LocalDateTime.now());
		paymentRepository.save(paymentToConfirm);

		// when: 결제 확정 (티켓 양도 실행)
		Payment confirmedPayment = paymentConfirmService.confirm(buyerId, new PaymentConfirmReq(orderId, tradePrice));

		// then: 결제 완료 상태
		assertThat(confirmedPayment.getStatus()).isEqualTo(PaymentStatus.DONE);

		// then: 양도 거래 완료 상태
		Trade completedTrade = tradeRepository.findById(savedTrade.getId()).orElseThrow();
		assertThat(completedTrade.getStatus()).isEqualTo(TradeStatus.COMPLETED);

		// then: 기존 티켓은 TRANSFERRED 상태
		Ticket transferredTicket = ticketRepository.findById(sellerTicket.getId()).orElseThrow();
		assertThat(transferredTicket.getStatus()).isEqualTo(TicketStatus.TRANSFERRED);

		// then: 구매자에게 새 티켓 발급
		List<Ticket> buyerTickets = ticketRepository.findAll().stream()
			.filter(t -> t.getMemberId().equals(buyerId))
			.toList();
		assertThat(buyerTickets).hasSize(1);
		assertThat(buyerTickets.get(0).getStatus()).isEqualTo(TicketStatus.ISSUED);
		assertThat(buyerTickets.get(0).getReservationId()).isEqualTo(reservationId);
		assertThat(buyerTickets.get(0).getSeatId()).isEqualTo(seatId);
	}

	@Test
	@DisplayName("본인 양도글은 결제 불가")
	void tradePayment_cannotBuyOwnTrade() {
		// given: 판매자의 티켓 생성
		Long sellerId = 1L;
		Long reservationId = 1L;
		Long seatId = 1L;

		Ticket sellerTicket = Ticket.builder()
			.reservationId(reservationId)
			.memberId(sellerId)
			.seatId(seatId)
			.qrCode("QR-SELLER-1")
			.build();
		ticketRepository.save(sellerTicket);

		// given: 양도글 생성
		Trade trade = Trade.builder()
			.memberId(sellerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(sellerTicket.getId())
			.type(TradeType.TRANSFER)
			.price(50000)
			.totalCount(1)
			.section("A구역")
			.row("5열")
			.seatNumber("12")
			.build();
		Trade savedTrade = tradeRepository.save(trade);

		// when & then: 판매자 본인이 결제 시도 → 실패
		PaymentPrepareReq prepareReq = new PaymentPrepareReq(
			DomainType.TRADE,
			savedTrade.getId(),
			PaymentMethod.CARD
		);

		assertThatThrownBy(() -> paymentPrepareService.prepare(sellerId, prepareReq))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("본인의 양도글은 구매할 수 없습니다");
	}

	@Test
	@DisplayName("EXCHANGE 타입은 결제 불가")
	void tradePayment_cannotPayForExchange() {
		// given: 판매자의 티켓 생성
		Long sellerId = 1L;
		Long buyerId = 2L;
		Long reservationId = 1L;
		Long seatId = 1L;

		Ticket sellerTicket = Ticket.builder()
			.reservationId(reservationId)
			.memberId(sellerId)
			.seatId(seatId)
			.qrCode("QR-SELLER-1")
			.build();
		ticketRepository.save(sellerTicket);

		// given: 교환글 생성 (가격 없음)
		Trade trade = Trade.builder()
			.memberId(sellerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(sellerTicket.getId())
			.type(TradeType.EXCHANGE)
			.price(null)
			.totalCount(1)
			.section("A구역")
			.row("5열")
			.seatNumber("12")
			.build();
		Trade savedTrade = tradeRepository.save(trade);

		// when & then: 교환글에 결제 시도 → 실패
		PaymentPrepareReq prepareReq = new PaymentPrepareReq(
			DomainType.TRADE,
			savedTrade.getId(),
			PaymentMethod.CARD
		);

		assertThatThrownBy(() -> paymentPrepareService.prepare(buyerId, prepareReq))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("양도(TRANSFER) 타입만 결제가 필요합니다");
	}

	@Test
	@DisplayName("동시에 여러 명이 같은 양도글 결제 시도 - 한 명만 성공")
	void tradePayment_concurrency_onlyOneSucceeds() throws InterruptedException {
		// given: 판매자의 티켓 생성
		Long sellerId = 1L;
		Long reservationId = 1L;
		Long seatId = 1L;
		Long tradePrice = 50000L;

		Ticket sellerTicket = Ticket.builder()
			.reservationId(reservationId)
			.memberId(sellerId)
			.seatId(seatId)
			.qrCode("QR-SELLER-1")
			.build();
		ticketRepository.save(sellerTicket);

		// given: 양도글 생성
		Trade trade = Trade.builder()
			.memberId(sellerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(sellerTicket.getId())
			.type(TradeType.TRANSFER)
			.price(tradePrice.intValue())
			.totalCount(1)
			.section("A구역")
			.row("5열")
			.seatNumber("12")
			.build();
		Trade savedTrade = tradeRepository.save(trade);

		// when: 10명이 동시에 결제 준비 시도
		int threadCount = 10;
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failCount = new AtomicInteger(0);

		for (int i = 0; i < threadCount; i++) {
			Long buyerId = 10L + i;
			executorService.submit(() -> {
				try {
					PaymentPrepareReq prepareReq = new PaymentPrepareReq(
						DomainType.TRADE,
						savedTrade.getId(),
						PaymentMethod.CARD
					);
					Payment preparedPayment = paymentPrepareService.prepare(buyerId, prepareReq);

					// 결제 승인까지 시도
					Payment paymentToConfirm = paymentRepository.findByOrderId(preparedPayment.getOrderId()).orElseThrow();
					paymentToConfirm.complete("PAYMENT-KEY-" + buyerId, java.time.LocalDateTime.now());
					paymentRepository.save(paymentToConfirm);

					paymentConfirmService.confirm(buyerId, new PaymentConfirmReq(
						preparedPayment.getOrderId(),
						tradePrice
					));

					successCount.incrementAndGet();
				} catch (Exception e) {
					failCount.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executorService.shutdown();

		// then: 한 명만 성공, 나머지는 실패
		assertThat(successCount.get()).isEqualTo(1);
		assertThat(failCount.get()).isEqualTo(threadCount - 1);

		// then: 거래는 COMPLETED 상태
		Trade completedTrade = tradeRepository.findById(savedTrade.getId()).orElseThrow();
		assertThat(completedTrade.getStatus()).isEqualTo(TradeStatus.COMPLETED);

		// then: 구매자 티켓 1개만 생성
		List<Ticket> buyerTickets = ticketRepository.findAll().stream()
			.filter(t -> !t.getMemberId().equals(sellerId))
			.filter(t -> t.getStatus() == TicketStatus.ISSUED)
			.toList();
		assertThat(buyerTickets).hasSize(1);
	}

	@Test
	@DisplayName("이미 완료된 거래는 재결제 불가")
	void tradePayment_cannotPayCompletedTrade() {
		// given: 판매자의 티켓 생성
		Long sellerId = 1L;
		Long buyerId = 2L;
		Long reservationId = 1L;
		Long seatId = 1L;

		Ticket sellerTicket = Ticket.builder()
			.reservationId(reservationId)
			.memberId(sellerId)
			.seatId(seatId)
			.qrCode("QR-SELLER-1")
			.build();
		ticketRepository.save(sellerTicket);

		// given: 이미 완료된 양도글
		Trade trade = Trade.builder()
			.memberId(sellerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(sellerTicket.getId())
			.type(TradeType.TRANSFER)
			.price(50000)
			.totalCount(1)
			.section("A구역")
			.row("5열")
			.seatNumber("12")
			.build();
		trade.complete(); // 완료 처리
		Trade savedTrade = tradeRepository.save(trade);

		// when & then: 완료된 거래 결제 시도 → 실패
		PaymentPrepareReq prepareReq = new PaymentPrepareReq(
			DomainType.TRADE,
			savedTrade.getId(),
			PaymentMethod.CARD
		);

		assertThatThrownBy(() -> paymentPrepareService.prepare(buyerId, prepareReq))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("유효하지 않은 거래 상태입니다");
	}

	@Test
	@DisplayName("멱등성 보장 - 동일 결제 재요청 시 안전하게 처리")
	void tradePayment_idempotency_safeRetry() {
		// given: 판매자의 티켓 생성
		Long sellerId = 1L;
		Long buyerId = 2L;
		Long reservationId = 1L;
		Long seatId = 1L;
		Long tradePrice = 50000L;

		Ticket sellerTicket = Ticket.builder()
			.reservationId(reservationId)
			.memberId(sellerId)
			.seatId(seatId)
			.qrCode("QR-SELLER-1")
			.build();
		ticketRepository.save(sellerTicket);

		// given: 양도글 생성
		Trade trade = Trade.builder()
			.memberId(sellerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(sellerTicket.getId())
			.type(TradeType.TRANSFER)
			.price(tradePrice.intValue())
			.totalCount(1)
			.section("A구역")
			.row("5열")
			.seatNumber("12")
			.build();
		Trade savedTrade = tradeRepository.save(trade);

		// given: 첫 번째 결제 완료
		PaymentPrepareReq prepareReq = new PaymentPrepareReq(
			DomainType.TRADE,
			savedTrade.getId(),
			PaymentMethod.CARD
		);
		Payment preparedPayment = paymentPrepareService.prepare(buyerId, prepareReq);
		String orderId = preparedPayment.getOrderId();

		Payment paymentToConfirm = paymentRepository.findByOrderId(orderId).orElseThrow();
		paymentToConfirm.complete("PAYMENT-KEY-123", java.time.LocalDateTime.now());
		paymentRepository.save(paymentToConfirm);

		paymentConfirmService.confirm(buyerId, new PaymentConfirmReq(orderId, tradePrice));

		// when: 동일한 결제 재확인 (멱등성 테스트)
		Payment retryConfirm = paymentConfirmService.confirm(buyerId, new PaymentConfirmReq(orderId, tradePrice));

		// then: 에러 없이 정상 처리
		assertThat(retryConfirm.getStatus()).isEqualTo(PaymentStatus.DONE);

		// then: 티켓은 여전히 1개만 존재 (중복 생성 없음)
		List<Ticket> buyerTickets = ticketRepository.findAll().stream()
			.filter(t -> t.getMemberId().equals(buyerId))
			.filter(t -> t.getStatus() == TicketStatus.ISSUED)
			.toList();
		assertThat(buyerTickets).hasSize(1);
	}
}
