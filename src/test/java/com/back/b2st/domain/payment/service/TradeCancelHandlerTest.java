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
import com.back.b2st.domain.ticket.repository.TicketRepository;
import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.entity.TradeType;
import com.back.b2st.domain.trade.error.TradeErrorCode;
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
	@DisplayName("handleCancel - Trade가 없으면 예외 발생")
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
			.isEqualTo(TradeErrorCode.TRADE_NOT_FOUND);
	}

	@Test
	@DisplayName("handleCancel - 이미 취소된 거래는 멱등성 보장")
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

		// when - 에러 없이 처리됨 (멱등성)
		assertThatCode(() -> tradeCancelHandler.handleCancel(payment))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("handleCancel - COMPLETED가 아닌 상태면 예외 발생")
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
			.hasMessageContaining("완료된 거래만 취소할 수 있습니다");
	}

	@Test
	@DisplayName("handleCancel - 구매자 티켓이 ISSUED가 아니면 예외 발생")
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
			.hasMessageContaining("사용/변경된 티켓은 결제 취소할 수 없습니다");
	}

	@Test
	@DisplayName("handleCancel - 정상 케이스: 티켓 복구 및 거래 취소")
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

		// when
		tradeCancelHandler.handleCancel(payment);

		// then - 거래가 취소됨
		Trade result = tradeRepository.findById(savedTrade.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(TradeStatus.CANCELLED);

		// then - 판매자 티켓 복구됨
		Ticket restoredTicket = ticketRepository.findById(sellerTicket.getId()).orElseThrow();
		assertThat(restoredTicket.getStatus()).isEqualTo(TicketStatus.ISSUED);

		// then - 구매자 티켓 삭제됨
		assertThat(ticketRepository.findById(buyerTicket.getId())).isEmpty();
	}

	@Test
	@DisplayName("handleCancel - 구매자 티켓이 없어도 정상 처리")
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

		// when - 에러 없이 처리됨
		assertThatCode(() -> tradeCancelHandler.handleCancel(payment))
			.doesNotThrowAnyException();

		// then - 거래가 취소됨
		Trade result = tradeRepository.findById(savedTrade.getId()).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(TradeStatus.CANCELLED);

		// then - 판매자 티켓 복구됨
		Ticket restoredTicket = ticketRepository.findById(sellerTicket.getId()).orElseThrow();
		assertThat(restoredTicket.getStatus()).isEqualTo(TicketStatus.ISSUED);
	}
}
