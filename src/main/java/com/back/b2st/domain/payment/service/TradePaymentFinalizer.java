package com.back.b2st.domain.payment.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.entity.TicketStatus;
import com.back.b2st.domain.ticket.error.TicketErrorCode;
import com.back.b2st.domain.ticket.service.TicketService;
import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.entity.TradeType;
import com.back.b2st.domain.trade.error.TradeErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TradePaymentFinalizer implements PaymentFinalizer {

	@PersistenceContext
	private EntityManager entityManager;

	private final TicketService ticketService;
	private final Clock clock;

	@Override
	public boolean supports(DomainType domainType) {
		return domainType == DomainType.TRADE;
	}

	@Override
	@Transactional
	public void finalizePayment(Payment payment) {
		// 1. Trade 조회 및 락 획득
		Trade trade = entityManager.find(Trade.class, payment.getDomainId(),
			LockModeType.PESSIMISTIC_WRITE);
		if (trade == null) {
			throw new BusinessException(TradeErrorCode.TRADE_NOT_FOUND);
		}

		// 2. TRANSFER 타입만 처리
		if (trade.getType() != TradeType.TRANSFER) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE, "양도 타입이 아닙니다.");
		}

		// 3. 본인 글이 아닌지 확인
		if (trade.getMemberId().equals(payment.getMemberId())) {
			throw new BusinessException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS, "본인의 양도글은 구매할 수 없습니다.");
		}

		// 4. 멱등성 처리: 이미 완료된 경우 검증만 하고 종료
		if (trade.getStatus() == TradeStatus.COMPLETED) {
			trade.ensureBuyer(payment.getMemberId(), LocalDateTime.now(clock));
			ensureTicketTransferred(trade, payment.getMemberId());
			return;
		}

		// 5. 취소된 경우 에러
		if (trade.getStatus() == TradeStatus.CANCELLED) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE, "취소된 거래입니다.");
		}

		// 6. ACTIVE 상태 검증
		if (trade.getStatus() != TradeStatus.ACTIVE) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE, "유효하지 않은 거래 상태입니다.");
		}

		// 7. 티켓 양도 처리
		handleTransfer(trade, payment.getMemberId());

		// 8. 거래 완료 처리
		trade.completeTransfer(payment.getMemberId(), LocalDateTime.now(clock));
	}

	private void handleTransfer(Trade trade, Long buyerId) {
		// 1. 기존 티켓 조회 및 검증
		Ticket oldTicket = ticketService.getTicketById(trade.getTicketId());
		validateTicketForTrade(oldTicket, trade);

		// 2. 기존 티켓을 TRANSFERRED 상태로 변경
		ticketService.transferTicket(
			oldTicket.getReservationId(),
			oldTicket.getMemberId(),
			oldTicket.getSeatId()
		);

		// 3. 구매자에게 새 티켓 생성 (원본 예약 정보 유지)
		ticketService.createTicket(
			oldTicket.getReservationId(),
			buyerId,
			oldTicket.getSeatId()
		);
	}

	private void validateTicketForTrade(Ticket ticket, Trade trade) {
		// 티켓이 양도 가능한 상태인지 검증
		if (ticket.getStatus() != TicketStatus.ISSUED) {
			throw new BusinessException(TicketErrorCode.TICKET_NOT_TRANSFERABLE);
		}

		// 티켓 소유자가 거래 등록자와 일치하는지 검증
		if (!ticket.getMemberId().equals(trade.getMemberId())) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE, "티켓 소유자가 일치하지 않습니다.");
		}
	}

	private void ensureTicketTransferred(Trade trade, Long buyerId) {
		// 멱등성 보장: 이미 완료된 경우 티켓 상태 검증
		Ticket oldTicket = ticketService.getTicketById(trade.getTicketId());

		// 기존 티켓이 TRANSFERRED 상태가 아니면 보정
		if (oldTicket.getStatus() != TicketStatus.TRANSFERRED) {
			ticketService.transferTicket(
				oldTicket.getReservationId(),
				oldTicket.getMemberId(),
				oldTicket.getSeatId()
			);
		}

		// 구매자에게 티켓이 생성되어 있는지 확인 (없으면 생성)
		ticketService.createTicket(
			oldTicket.getReservationId(),
			buyerId,
			oldTicket.getSeatId()
		);
	}
}
