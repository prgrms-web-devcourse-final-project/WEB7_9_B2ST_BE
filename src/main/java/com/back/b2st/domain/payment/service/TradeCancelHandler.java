package com.back.b2st.domain.payment.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.entity.TicketStatus;
import com.back.b2st.domain.ticket.repository.TicketRepository;
import com.back.b2st.domain.ticket.service.TicketService;
import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.error.TradeErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TradeCancelHandler implements PaymentCancelHandler {

	@PersistenceContext
	private EntityManager entityManager;

	private final TicketService ticketService;
	private final TicketRepository ticketRepository;

	@Override
	public boolean supports(DomainType domainType) {
		return domainType == DomainType.TRADE;
	}

	@Override
	@Transactional
	public void handleCancel(Payment payment) {
		// 1. Trade 조회 및 락 획득
		Trade trade = entityManager.find(Trade.class, payment.getDomainId(),
			LockModeType.PESSIMISTIC_WRITE);
		if (trade == null) {
			throw new BusinessException(TradeErrorCode.TRADE_NOT_FOUND);
		}

		// 2. 이미 취소된 경우 멱등성 처리
		if (trade.getStatus() == TradeStatus.CANCELLED) {
			return;
		}

		// 3. COMPLETED 상태만 취소 가능
		if (trade.getStatus() != TradeStatus.COMPLETED) {
			throw new BusinessException(PaymentErrorCode.INVALID_STATUS,
				"완료된 거래만 취소할 수 있습니다.");
		}

		// 4. 원본(판매자) 티켓 조회
		Ticket originalTicket = ticketService.getTicketById(trade.getTicketId());
		Long reservationId = originalTicket.getReservationId();
		Long seatId = originalTicket.getSeatId();

		// 5. 구매자 티켓 삭제 (양도받은 티켓)
		Long buyerId = payment.getMemberId();
		ticketRepository.findByReservationIdAndMemberIdAndSeatId(reservationId, buyerId, seatId)
			.ifPresent(ticket -> {
				if (ticket.getStatus() != TicketStatus.ISSUED) {
					throw new BusinessException(PaymentErrorCode.INVALID_STATUS,
						"사용/변경된 티켓은 결제 취소할 수 없습니다.");
				}
				ticketRepository.delete(ticket);
			});

		// 6. 판매자 티켓 복구 (TRANSFERRED → ISSUED)
		if (originalTicket.getStatus() == TicketStatus.TRANSFERRED) {
			ticketService.restoreTicket(originalTicket.getId());
		}

		// 7. 거래 상태를 CANCELLED로 변경
		trade.cancel();
	}
}
