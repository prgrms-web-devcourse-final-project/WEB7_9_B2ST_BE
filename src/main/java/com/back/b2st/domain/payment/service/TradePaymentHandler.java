package com.back.b2st.domain.payment.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.payment.entity.DomainType;
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
public class TradePaymentHandler implements PaymentDomainHandler {

	@PersistenceContext
	private EntityManager entityManager;

	private final TicketService ticketService;

	@Override
	public boolean supports(DomainType domainType) {
		return domainType == DomainType.TRADE;
	}

	@Override
	@Transactional
	public PaymentTarget loadAndValidate(Long tradeId, Long memberId) {
		// 1. Trade 조회 및 비관적 락 획득 (동시성 제어)
		Trade trade = entityManager.find(Trade.class, tradeId, LockModeType.PESSIMISTIC_WRITE);
		if (trade == null) {
			throw new BusinessException(TradeErrorCode.TRADE_NOT_FOUND);
		}

		// 2. TRANSFER 타입만 결제 가능 (EXCHANGE는 무료 교환)
		if (trade.getType() != TradeType.TRANSFER) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE, "양도(TRANSFER) 타입만 결제가 필요합니다.");
		}

		// 3. 본인 글은 결제 불가
		if (trade.getMemberId().equals(memberId)) {
			throw new BusinessException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS, "본인의 양도글은 구매할 수 없습니다.");
		}

		// 4. ACTIVE 상태만 결제 가능
		if (trade.getStatus() != TradeStatus.ACTIVE) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE, "유효하지 않은 거래 상태입니다.");
		}

		// 5. 가격이 설정되어 있어야 함
		if (trade.getPrice() == null || trade.getPrice() <= 0) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE, "거래 가격이 설정되지 않았습니다.");
		}

		// 6. 티켓 상태 사전 검증 (결제 준비 시점에 검증하여 UX 개선)
		Ticket ticket = ticketService.getTicketById(trade.getTicketId());
		validateTicketForPayment(ticket, trade);

		Long expectedAmount = trade.getPrice().longValue();
		return new PaymentTarget(DomainType.TRADE, tradeId, expectedAmount);
	}

	private void validateTicketForPayment(Ticket ticket, Trade trade) {
		// 티켓이 양도 가능한 상태인지 검증
		if (ticket.getStatus() != TicketStatus.ISSUED) {
			throw new BusinessException(TicketErrorCode.TICKET_NOT_TRANSFERABLE);
		}

		// 티켓 소유자가 거래 등록자와 일치하는지 검증
		if (!ticket.getMemberId().equals(trade.getMemberId())) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE, "티켓 소유자가 일치하지 않습니다.");
		}
	}
}
