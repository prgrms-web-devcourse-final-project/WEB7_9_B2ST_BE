package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.back.b2st.domain.payment.dto.request.PaymentCancelReq;
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
class PaymentCancelServiceTest {

	@Autowired
	private PaymentCancelService paymentCancelService;

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
	@DisplayName("티켓 거래(TRADE) 결제는 취소/환불 불가")
	void cancel_tradeNotRefundable() {
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
		sellerTicket.transfer(); // 양도 완료 상태
		ticketRepository.save(sellerTicket);

		// given: 구매자에게 발급된 티켓
		Ticket buyerTicket = Ticket.builder()
			.reservationId(reservationId)
			.memberId(buyerId)
			.seatId(seatId)
			.qrCode("QR-BUYER-1")
			.build();
		ticketRepository.save(buyerTicket);

		// given: 완료된 양도 거래
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
		trade.complete();
		Trade savedTrade = tradeRepository.save(trade);

		// given: 완료된 결제
		Payment payment = Payment.builder()
			.orderId("ORDER-CANCEL-1")
			.memberId(buyerId)
			.domainType(DomainType.TRADE)
			.domainId(savedTrade.getId())
			.amount(50000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete("PAYMENT-KEY-123", java.time.LocalDateTime.now());
		paymentRepository.save(payment);

		// when & then: 결제 취소 불가
		PaymentCancelReq cancelReq = new PaymentCancelReq("구매자 변심");
		assertThatThrownBy(() -> paymentCancelService.cancel(buyerId, "ORDER-CANCEL-1", cancelReq))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("티켓 거래 결제는 취소/환불을 지원하지 않습니다");

		// then: 도메인 상태는 변경되지 않음
		Payment persisted = paymentRepository.findByOrderId("ORDER-CANCEL-1").orElseThrow();
		assertThat(persisted.getStatus()).isEqualTo(PaymentStatus.DONE);

		Trade persistedTrade = tradeRepository.findById(savedTrade.getId()).orElseThrow();
		assertThat(persistedTrade.getStatus()).isEqualTo(TradeStatus.COMPLETED);

		Ticket persistedSellerTicket = ticketRepository.findById(sellerTicket.getId()).orElseThrow();
		assertThat(persistedSellerTicket.getStatus()).isEqualTo(TicketStatus.TRANSFERRED);

		assertThat(ticketRepository.findById(buyerTicket.getId())).isPresent();
	}

	@Test
	@DisplayName("본인 결제만 취소 가능")
	void cancel_onlyOwnerCanCancel() {
		// given: 구매자의 결제
		Long buyerId = 2L;
		Long otherId = 999L;

		Payment payment = Payment.builder()
			.orderId("ORDER-CANCEL-2")
			.memberId(buyerId)
			.domainType(DomainType.TRADE)
			.domainId(1L)
			.amount(50000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete("PAYMENT-KEY-123", java.time.LocalDateTime.now());
		paymentRepository.save(payment);

		// when & then: 다른 사용자가 취소 시도 → 실패
		PaymentCancelReq cancelReq = new PaymentCancelReq("구매자 변심");
		assertThatThrownBy(() -> paymentCancelService.cancel(otherId, "ORDER-CANCEL-2", cancelReq))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("권한이 없습니다");
	}

	@Test
	@DisplayName("DONE 상태만 취소 가능")
	void cancel_onlyDoneStatusCanBeCanceled() {
		// given: READY 상태의 결제
		Long buyerId = 2L;

		Payment payment = Payment.builder()
			.orderId("ORDER-CANCEL-3")
			.memberId(buyerId)
			.domainType(DomainType.TRADE)
			.domainId(1L)
			.amount(50000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		// READY 상태 (완료되지 않음)
		paymentRepository.save(payment);

		// when & then: READY 상태 취소 시도 → 실패
		PaymentCancelReq cancelReq = new PaymentCancelReq("구매자 변심");
		assertThatThrownBy(() -> paymentCancelService.cancel(buyerId, "ORDER-CANCEL-3", cancelReq))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("완료된 결제만 취소할 수 있습니다");
	}

	@Test
	@DisplayName("멱등성 보장 - 이미 취소된 결제 재취소 시도")
	void cancel_idempotency_alreadyCanceledPayment() {
		// given: 이미 취소된 결제
		Long buyerId = 2L;

		Payment payment = Payment.builder()
			.orderId("ORDER-CANCEL-4")
			.memberId(buyerId)
			.domainType(DomainType.TRADE)
			.domainId(1L)
			.amount(50000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete("PAYMENT-KEY-123", java.time.LocalDateTime.now());
		payment.cancel("이미 취소됨", java.time.LocalDateTime.now());
		paymentRepository.save(payment);

		// when: 이미 취소된 결제 재취소 시도
		PaymentCancelReq cancelReq = new PaymentCancelReq("구매자 변심");
		Payment result = paymentCancelService.cancel(buyerId, "ORDER-CANCEL-4", cancelReq);

		// then: 에러 없이 정상 처리 (멱등성)
		assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELED);
	}

	@Test
	@DisplayName("존재하지 않는 결제 취소 시도")
	void cancel_paymentNotFound() {
		// given
		Long buyerId = 2L;

		// when & then: 존재하지 않는 orderId로 취소 시도
		PaymentCancelReq cancelReq = new PaymentCancelReq("구매자 변심");
		assertThatThrownBy(() -> paymentCancelService.cancel(buyerId, "NON-EXISTENT-ORDER", cancelReq))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("결제 정보를 찾을 수 없습니다");
	}

	@Test
	@DisplayName("예매(RESERVATION) 결제는 취소/환불 불가")
	void cancel_reservationNotRefundable() {
		// given: RESERVATION 타입 결제
		Long buyerId = 2L;

		Payment payment = Payment.builder()
			.orderId("ORDER-CANCEL-5")
			.memberId(buyerId)
			.domainType(DomainType.RESERVATION)
			.domainId(1L)
			.amount(50000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete("PAYMENT-KEY-456", java.time.LocalDateTime.now());
		paymentRepository.save(payment);

		// when & then: 핸들러가 없는 도메인 취소 시도
		PaymentCancelReq cancelReq = new PaymentCancelReq("구매자 변심");
		assertThatThrownBy(() -> paymentCancelService.cancel(buyerId, "ORDER-CANCEL-5", cancelReq))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("예매 결제는 취소/환불을 지원하지 않습니다");
	}
}
