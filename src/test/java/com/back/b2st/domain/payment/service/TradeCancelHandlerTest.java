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
import com.back.b2st.domain.ticket.repository.TicketRepository;
import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeType;
import com.back.b2st.domain.trade.repository.TradeRepository;
import com.back.b2st.global.error.exception.BusinessException;

@SpringBootTest
@ActiveProfiles("test")
class TradeCancelHandlerTest {

	@Autowired
	private TradeCancelHandler tradeCancelHandler;

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
		assertThat(tradeCancelHandler.supports(DomainType.TRADE)).isTrue();
	}

	@Test
	@DisplayName("supports - 다른 도메인 미지원")
	void supports_other() {
		assertThat(tradeCancelHandler.supports(DomainType.RESERVATION)).isFalse();
		assertThat(tradeCancelHandler.supports(DomainType.LOTTERY)).isFalse();
	}

	@Test
	@DisplayName("handleCancel - 티켓 거래 결제는 취소/환불 미지원")
	void handleCancel_tradeNotFound() {
		// given
		Payment payment = Payment.builder()
			.orderId("ORDER-1")
			.memberId(2L)
			.domainType(DomainType.TRADE)
			.domainId(999L) // 존재하지 않는 Trade
			.amount(50000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete("PAYMENT-KEY-1", java.time.LocalDateTime.now());
		paymentRepository.save(payment);

		// when & then
		assertThatThrownBy(() -> tradeCancelHandler.handleCancel(payment))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException) ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
	}

	@Test
	@DisplayName("handleCancel - 티켓 거래 결제는 취소/환불 미지원 (이미 취소된 거래여도 동일)")
	void handleCancel_alreadyCancelled() {
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
			.type(TradeType.TRANSFER)
			.price(50000)
			.totalCount(1)
			.section("A")
			.row("5")
			.seatNumber("12")
			.build();
		trade.cancel(); // 이미 취소됨
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

		assertThatThrownBy(() -> tradeCancelHandler.handleCancel(payment))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException) ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
	}

	@Test
	@DisplayName("handleCancel - 티켓 거래 결제는 취소/환불 미지원 (상태와 무관)")
	void handleCancel_notCompletedStatus() {
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
		// ACTIVE 상태 (COMPLETED가 아님)
		Trade savedTrade = tradeRepository.save(trade);

		Payment payment = Payment.builder()
			.orderId("ORDER-3")
			.memberId(2L)
			.domainType(DomainType.TRADE)
			.domainId(savedTrade.getId())
			.amount(50000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete("PAYMENT-KEY-3", java.time.LocalDateTime.now());
		paymentRepository.save(payment);

		// when & then
		assertThatThrownBy(() -> tradeCancelHandler.handleCancel(payment))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException) ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
	}

	@Test
	@DisplayName("handleCancel - 티켓 거래 결제는 취소/환불 미지원 (티켓 상태와 무관)")
	void handleCancel_buyerTicketNotIssued() {
		// given
		Long sellerId = 1L;
		Long buyerId = 2L;

		Ticket sellerTicket = Ticket.builder()
			.reservationId(1L)
			.memberId(sellerId)
			.seatId(1L)
			.qrCode("QR-SELLER")
			.build();
		sellerTicket.transfer();
		ticketRepository.save(sellerTicket);

		Ticket buyerTicket = Ticket.builder()
			.reservationId(1L)
			.memberId(buyerId)
			.seatId(1L)
			.qrCode("QR-BUYER")
			.build();
		buyerTicket.use(); // USED 상태
		ticketRepository.save(buyerTicket);

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
		trade.complete();
		Trade savedTrade = tradeRepository.save(trade);

		Payment payment = Payment.builder()
			.orderId("ORDER-4")
			.memberId(buyerId)
			.domainType(DomainType.TRADE)
			.domainId(savedTrade.getId())
			.amount(50000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete("PAYMENT-KEY-4", java.time.LocalDateTime.now());
		paymentRepository.save(payment);

		// when & then
		assertThatThrownBy(() -> tradeCancelHandler.handleCancel(payment))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException) ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
	}

	@Test
	@DisplayName("handleCancel - 티켓 거래 결제는 취소/환불 미지원")
	void handleCancel_success() {
		// given
		Long sellerId = 1L;
		Long buyerId = 2L;

		Ticket sellerTicket = Ticket.builder()
			.reservationId(1L)
			.memberId(sellerId)
			.seatId(1L)
			.qrCode("QR-SELLER")
			.build();
		sellerTicket.transfer();
		ticketRepository.save(sellerTicket);

		Ticket buyerTicket = Ticket.builder()
			.reservationId(1L)
			.memberId(buyerId)
			.seatId(1L)
			.qrCode("QR-BUYER")
			.build();
		ticketRepository.save(buyerTicket);

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
		trade.complete();
		Trade savedTrade = tradeRepository.save(trade);

		Payment payment = Payment.builder()
			.orderId("ORDER-5")
			.memberId(buyerId)
			.domainType(DomainType.TRADE)
			.domainId(savedTrade.getId())
			.amount(50000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete("PAYMENT-KEY-5", java.time.LocalDateTime.now());
		paymentRepository.save(payment);

		assertThatThrownBy(() -> tradeCancelHandler.handleCancel(payment))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException) ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
	}

	@Test
	@DisplayName("handleCancel - 티켓 거래 결제는 취소/환불 미지원 (구매자 티켓 유무와 무관)")
	void handleCancel_noBuyerTicket() {
		// given
		Long sellerId = 1L;
		Long buyerId = 2L;

		Ticket sellerTicket = Ticket.builder()
			.reservationId(1L)
			.memberId(sellerId)
			.seatId(1L)
			.qrCode("QR-SELLER")
			.build();
		sellerTicket.transfer();
		ticketRepository.save(sellerTicket);

		// 구매자 티켓 없음

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
		trade.complete();
		Trade savedTrade = tradeRepository.save(trade);

		Payment payment = Payment.builder()
			.orderId("ORDER-6")
			.memberId(buyerId)
			.domainType(DomainType.TRADE)
			.domainId(savedTrade.getId())
			.amount(50000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete("PAYMENT-KEY-6", java.time.LocalDateTime.now());
		paymentRepository.save(payment);

		assertThatThrownBy(() -> tradeCancelHandler.handleCancel(payment))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException) ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
	}
}
