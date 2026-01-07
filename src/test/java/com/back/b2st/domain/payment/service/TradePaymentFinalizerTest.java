package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentMethod;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.payment.repository.PaymentRepository;
import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.entity.TicketStatus;
import com.back.b2st.domain.ticket.error.TicketErrorCode;
import com.back.b2st.domain.ticket.repository.TicketRepository;
import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.entity.TradeType;
import com.back.b2st.domain.trade.error.TradeErrorCode;
import com.back.b2st.domain.trade.repository.TradeRepository;
import com.back.b2st.global.error.exception.BusinessException;

@SpringBootTest
@ActiveProfiles("test")
class TradePaymentFinalizerTest {

	@Autowired
	private TradePaymentFinalizer tradePaymentFinalizer;

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
	@DisplayName("supports - TRADE 도메인 지원")
	void supports_trade() {
		assertThat(tradePaymentFinalizer.supports(DomainType.TRADE)).isTrue();
	}

	@Test
	@DisplayName("supports - 다른 도메인 미지원")
	void supports_other() {
		assertThat(tradePaymentFinalizer.supports(DomainType.RESERVATION)).isFalse();
		assertThat(tradePaymentFinalizer.supports(DomainType.LOTTERY)).isFalse();
	}

	@Test
	@DisplayName("finalizePayment - Trade가 없으면 예외 발생")
	void finalizePayment_tradeNotFound() {
		// given
		Payment payment = Payment.builder()
			.orderId("ORDER-1")
			.memberId(2L)
			.domainType(DomainType.TRADE)
			.domainId(999L) // 존재하지 않는 Trade ID
			.amount(50000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete("PAYMENT-KEY-1", java.time.LocalDateTime.now());
		paymentRepository.save(payment);

		// when & then
		assertThatThrownBy(() -> tradePaymentFinalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException) ex).getErrorCode())
			.isEqualTo(TradeErrorCode.TRADE_NOT_FOUND);
	}

	@Test
	@DisplayName("finalizePayment - EXCHANGE 타입이면 예외 발생")
	void finalizePayment_notTransferType() {
		// given
		Long sellerId = 1L;
		Ticket ticket = Ticket.builder()
			.reservationId(1L)
			.memberId(sellerId)
			.seatId(1L)
			.qrCode("QR-1")
			.build();
		ticketRepository.save(ticket);

		Trade trade = Trade.builder()
			.memberId(sellerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(ticket.getId())
			.type(TradeType.EXCHANGE) // TRANSFER가 아님
			.price(null)
			.totalCount(1)
			.section("A")
			.row("5")
			.seatNumber("12")
			.build();
		Trade savedTrade = tradeRepository.save(trade);

		Payment payment = Payment.builder()
			.orderId("ORDER-2")
			.memberId(2L)
			.domainType(DomainType.TRADE)
			.domainId(savedTrade.getId())
			.amount(50000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete("PAYMENT-KEY-2", java.time.LocalDateTime.now());
		paymentRepository.save(payment);

		// when & then
		assertThatThrownBy(() -> tradePaymentFinalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("양도 타입이 아닙니다");
	}

	@Test
	@DisplayName("finalizePayment - 본인 글이면 예외 발생")
	void finalizePayment_ownTrade() {
		// given
		Long sellerId = 1L;
		Ticket ticket = Ticket.builder()
			.reservationId(1L)
			.memberId(sellerId)
			.seatId(1L)
			.qrCode("QR-3")
			.build();
		ticketRepository.save(ticket);

		Trade trade = Trade.builder()
			.memberId(sellerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(ticket.getId())
			.type(TradeType.TRANSFER)
			.price(50000)
			.totalCount(1)
			.section("A")
			.row("5")
			.seatNumber("12")
			.build();
		Trade savedTrade = tradeRepository.save(trade);

		Payment payment = Payment.builder()
			.orderId("ORDER-3")
			.memberId(sellerId) // 본인
			.domainType(DomainType.TRADE)
			.domainId(savedTrade.getId())
			.amount(50000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete("PAYMENT-KEY-3", java.time.LocalDateTime.now());
		paymentRepository.save(payment);

		// when & then
		assertThatThrownBy(() -> tradePaymentFinalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("본인의 양도글은 구매할 수 없습니다");
	}

	@Test
	@DisplayName("finalizePayment - 취소된 거래면 예외 발생")
	void finalizePayment_cancelledTrade() {
		// given
		Long sellerId = 1L;
		Ticket ticket = Ticket.builder()
			.reservationId(1L)
			.memberId(sellerId)
			.seatId(1L)
			.qrCode("QR-4")
			.build();
		ticketRepository.save(ticket);

		Trade trade = Trade.builder()
			.memberId(sellerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(ticket.getId())
			.type(TradeType.TRANSFER)
			.price(50000)
			.totalCount(1)
			.section("A")
			.row("5")
			.seatNumber("12")
			.build();
		trade.cancel(); // 취소됨
		Trade savedTrade = tradeRepository.save(trade);

		Payment payment = Payment.builder()
			.orderId("ORDER-4")
			.memberId(2L)
			.domainType(DomainType.TRADE)
			.domainId(savedTrade.getId())
			.amount(50000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete("PAYMENT-KEY-4", java.time.LocalDateTime.now());
		paymentRepository.save(payment);

		// when & then
		assertThatThrownBy(() -> tradePaymentFinalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("취소된 거래입니다");
	}

	@Test
	@DisplayName("finalizePayment - 티켓이 ISSUED가 아니면 예외 발생")
	void finalizePayment_ticketNotIssued() {
		// given
		Long sellerId = 1L;
		Ticket ticket = Ticket.builder()
			.reservationId(1L)
			.memberId(sellerId)
			.seatId(1L)
			.qrCode("QR-5")
			.build();
		ticket.use(); // USED 상태로 변경
		ticketRepository.save(ticket);

		Trade trade = Trade.builder()
			.memberId(sellerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(ticket.getId())
			.type(TradeType.TRANSFER)
			.price(50000)
			.totalCount(1)
			.section("A")
			.row("5")
			.seatNumber("12")
			.build();
		Trade savedTrade = tradeRepository.save(trade);

		Payment payment = Payment.builder()
			.orderId("ORDER-5")
			.memberId(2L)
			.domainType(DomainType.TRADE)
			.domainId(savedTrade.getId())
			.amount(50000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete("PAYMENT-KEY-5", java.time.LocalDateTime.now());
		paymentRepository.save(payment);

		// when & then
		assertThatThrownBy(() -> tradePaymentFinalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException) ex).getErrorCode())
			.isEqualTo(TicketErrorCode.TICKET_NOT_TRANSFERABLE);
	}

	@Test
	@DisplayName("finalizePayment - 티켓 소유자가 거래 등록자와 다르면 예외 발생")
	void finalizePayment_ticketOwnerMismatch() {
		// given
		Long sellerId = 1L;
		Long actualOwnerId = 999L;

		Ticket ticket = Ticket.builder()
			.reservationId(1L)
			.memberId(actualOwnerId) // 다른 사람 소유
			.seatId(1L)
			.qrCode("QR-6")
			.build();
		ticketRepository.save(ticket);

		Trade trade = Trade.builder()
			.memberId(sellerId) // 판매자
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(ticket.getId())
			.type(TradeType.TRANSFER)
			.price(50000)
			.totalCount(1)
			.section("A")
			.row("5")
			.seatNumber("12")
			.build();
		Trade savedTrade = tradeRepository.save(trade);

		Payment payment = Payment.builder()
			.orderId("ORDER-6")
			.memberId(2L)
			.domainType(DomainType.TRADE)
			.domainId(savedTrade.getId())
			.amount(50000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete("PAYMENT-KEY-6", java.time.LocalDateTime.now());
		paymentRepository.save(payment);

		// when & then
		assertThatThrownBy(() -> tradePaymentFinalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("티켓 소유자가 일치하지 않습니다");
	}

	@Test
	@DisplayName("finalizePayment - 정상 케이스: ACTIVE 거래를 완료 처리")
	void finalizePayment_normalCase_success() {
		// given: ACTIVE 상태의 거래 (아직 완료되지 않음)
		Long sellerId = 1L;
		Long buyerId = 2L;

		Ticket sellerTicket = Ticket.builder()
			.reservationId(1L)
			.memberId(sellerId)
			.seatId(1L)
			.qrCode("QR-7")
			.build();
		ticketRepository.save(sellerTicket);

		Trade trade = Trade.builder()
			.memberId(sellerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(sellerTicket.getId())
			.type(TradeType.TRANSFER)
			.price(50000)
			.totalCount(1)
			.section("A")
			.row("5")
			.seatNumber("12")
			.build();
		// ACTIVE 상태 (완료되지 않음)
		Trade savedTrade = tradeRepository.save(trade);

		Payment payment = Payment.builder()
			.orderId("ORDER-7")
			.memberId(buyerId)
			.domainType(DomainType.TRADE)
			.domainId(savedTrade.getId())
			.amount(50000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete("PAYMENT-KEY-7", java.time.LocalDateTime.now());
		paymentRepository.save(payment);

		// when
		tradePaymentFinalizer.finalizePayment(payment);

		// then - 거래가 완료됨
		Trade result = tradeRepository.findById(savedTrade.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(TradeStatus.COMPLETED);
		assertThat(result.getBuyerId()).isEqualTo(buyerId);
		assertThat(result.getPurchasedAt()).isNotNull();

		// then - 판매자 티켓이 TRANSFERRED로 변경됨
		Ticket updatedSellerTicket = ticketRepository.findById(sellerTicket.getId()).orElseThrow();
		assertThat(updatedSellerTicket.getStatus()).isEqualTo(TicketStatus.TRANSFERRED);

		// then - 구매자 티켓이 생성됨
		Ticket buyerTicket = ticketRepository.findByReservationIdAndMemberIdAndSeatId(1L, buyerId, 1L)
			.orElseThrow();
		assertThat(buyerTicket.getStatus()).isEqualTo(TicketStatus.ISSUED);
	}

	@Test
	@DisplayName("finalizePayment - 이미 완료된 거래는 멱등성 보장하며 정상 처리")
	void finalizePayment_alreadyCompleted_idempotent() {
		// given
		Long sellerId = 1L;
		Long buyerId = 2L;

		Ticket sellerTicket = Ticket.builder()
			.reservationId(1L)
			.memberId(sellerId)
			.seatId(1L)
			.qrCode("QR-8")
			.build();
		sellerTicket.transfer(); // 이미 TRANSFERRED
		ticketRepository.save(sellerTicket);

		Trade trade = Trade.builder()
			.memberId(sellerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(sellerTicket.getId())
			.type(TradeType.TRANSFER)
			.price(50000)
			.totalCount(1)
			.section("A")
			.row("5")
			.seatNumber("12")
			.build();
		trade.complete(); // 이미 완료됨
		Trade savedTrade = tradeRepository.save(trade);

		Payment payment = Payment.builder()
			.orderId("ORDER-8")
			.memberId(buyerId)
			.domainType(DomainType.TRADE)
			.domainId(savedTrade.getId())
			.amount(50000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete("PAYMENT-KEY-8", java.time.LocalDateTime.now());
		paymentRepository.save(payment);

		// when - 에러 없이 처리됨 (멱등성)
		assertThatCode(() -> tradePaymentFinalizer.finalizePayment(payment))
			.doesNotThrowAnyException();

		// then - 거래는 여전히 COMPLETED
		Trade result = tradeRepository.findById(savedTrade.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(TradeStatus.COMPLETED);
		assertThat(result.getBuyerId()).isEqualTo(buyerId);
		assertThat(result.getPurchasedAt()).isNotNull();
	}

	@Test
	@DisplayName("finalizePayment - 멱등성 보정: COMPLETED 거래에 ISSUED 티켓이 있으면 자동 보정")
	void finalizePayment_alreadyCompleted_correctsIssuedTicket() {
		// given: 거래는 완료되었지만 티켓이 ISSUED 상태 (양도 처리 실패한 경우)
		Long sellerId = 1L;
		Long buyerId = 2L;

		Ticket sellerTicket = Ticket.builder()
			.reservationId(1L)
			.memberId(sellerId)
			.seatId(1L)
			.qrCode("QR-9")
			.build();
		// ISSUED 상태로 유지 (transfer() 호출 안 함)
		ticketRepository.save(sellerTicket);

		Trade trade = Trade.builder()
			.memberId(sellerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(sellerTicket.getId())
			.type(TradeType.TRANSFER)
			.price(50000)
			.totalCount(1)
			.section("A")
			.row("5")
			.seatNumber("12")
			.build();
		trade.complete(); // 거래는 완료됨
		Trade savedTrade = tradeRepository.save(trade);

		Payment payment = Payment.builder()
			.orderId("ORDER-9")
			.memberId(buyerId)
			.domainType(DomainType.TRADE)
			.domainId(savedTrade.getId())
			.amount(50000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete("PAYMENT-KEY-9", java.time.LocalDateTime.now());
		paymentRepository.save(payment);

		// when - 멱등성 보장하며 티켓 상태 보정
		assertThatCode(() -> tradePaymentFinalizer.finalizePayment(payment))
			.doesNotThrowAnyException();

		// then - 판매자 티켓이 자동으로 TRANSFERRED로 변경됨
		Ticket updatedSellerTicket = ticketRepository.findById(sellerTicket.getId()).orElseThrow();
		assertThat(updatedSellerTicket.getStatus()).isEqualTo(TicketStatus.TRANSFERRED);

		// then - 구매자 티켓이 생성됨
		Ticket buyerTicket = ticketRepository.findByReservationIdAndMemberIdAndSeatId(1L, buyerId, 1L)
			.orElseThrow();
		assertThat(buyerTicket.getStatus()).isEqualTo(TicketStatus.ISSUED);

		Trade result = tradeRepository.findById(savedTrade.getId()).orElseThrow();
		assertThat(result.getBuyerId()).isEqualTo(buyerId);
		assertThat(result.getPurchasedAt()).isNotNull();
	}
}
