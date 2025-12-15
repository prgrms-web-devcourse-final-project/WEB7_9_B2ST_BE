package com.back.b2st.domain.trade.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.entity.TicketStatus;
import com.back.b2st.domain.ticket.error.TicketErrorCode;
import com.back.b2st.domain.ticket.service.TicketService;
import com.back.b2st.domain.trade.dto.request.CreateTradeRequestReq;
import com.back.b2st.domain.trade.dto.response.TradeRequestRes;
import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeRequest;
import com.back.b2st.domain.trade.entity.TradeRequestStatus;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.entity.TradeType;
import com.back.b2st.domain.trade.error.TradeErrorCode;
import com.back.b2st.domain.trade.repository.TradeRepository;
import com.back.b2st.domain.trade.repository.TradeRequestRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeRequestService {

	private final TradeRequestRepository tradeRequestRepository;
	private final TradeRepository tradeRepository;
	private final TicketService ticketService;

	@Transactional
	public TradeRequestRes createTradeRequest(Long tradeId, CreateTradeRequestReq request,
		Long requesterId) {
		Trade trade = findTradeById(tradeId);

		validateTradeIsActive(trade);
		validateNotOwnTrade(trade, requesterId);
		validateNoDuplicateRequest(trade, requesterId);

		TradeRequest tradeRequest = TradeRequest.builder()
			.trade(trade)
			.requesterId(requesterId)
			.requesterTicketId(request.getRequesterTicketId())
			.build();

		TradeRequest savedRequest = tradeRequestRepository.save(tradeRequest);
		return TradeRequestRes.from(savedRequest);
	}

	public TradeRequestRes getTradeRequest(Long tradeRequestId) {
		TradeRequest tradeRequest = findTradeRequestById(tradeRequestId);
		return TradeRequestRes.from(tradeRequest);
	}

	public List<TradeRequestRes> getTradeRequestsByTrade(Long tradeId) {
		Trade trade = findTradeById(tradeId);
		List<TradeRequest> requests = tradeRequestRepository.findByTrade(trade);
		return requests.stream()
			.map(TradeRequestRes::from)
			.collect(Collectors.toList());
	}

	public List<TradeRequestRes> getTradeRequestsByRequester(Long requesterId) {
		List<TradeRequest> requests = tradeRequestRepository.findByRequesterId(requesterId);
		return requests.stream()
			.map(TradeRequestRes::from)
			.collect(Collectors.toList());
	}

	@Transactional
	public void acceptTradeRequest(Long tradeRequestId, Long memberId) {
		TradeRequest tradeRequest = findTradeRequestById(tradeRequestId);
		Trade trade = tradeRequest.getTrade();

		validateTradeOwner(trade, memberId);
		validateTradeIsActive(trade);
		validateTradeRequestIsPending(tradeRequest);
		validateNoAcceptedRequests(trade);

		tradeRequest.accept();
		trade.complete();

		// 티켓 소유권 이전 처리
		transferTicketOwnership(trade, tradeRequest);
	}

	private void transferTicketOwnership(Trade trade, TradeRequest tradeRequest) {
		if (trade.getType() == TradeType.TRANSFER) {
			handleTransfer(trade, tradeRequest);
		} else if (trade.getType() == TradeType.EXCHANGE) {
			handleExchange(trade, tradeRequest);
		}
	}

	private void handleTransfer(Trade trade, TradeRequest tradeRequest) {
		// 1. 기존 티켓 조회 및 검증
		Ticket oldTicket = ticketService.getTicketById(trade.getTicketId());
		validateTicketForTrade(oldTicket);

		// 2. 기존 티켓을 TRANSFERRED 상태로 변경
		ticketService.transferTicket(
			oldTicket.getReservationId(),
			oldTicket.getMemberId(),
			oldTicket.getSeatId()
		);

		// 3. 신청자에게 새 티켓 생성 (원본 예약 정보 유지)
		ticketService.createTicket(
			oldTicket.getReservationId(),
			tradeRequest.getRequesterId(),
			oldTicket.getSeatId()
		);
	}

	private void handleExchange(Trade trade, TradeRequest tradeRequest) {
		// 1. 양쪽 티켓 조회 및 검증
		Ticket ownerTicket = ticketService.getTicketById(trade.getTicketId());
		Ticket requesterTicket = ticketService.getTicketById(tradeRequest.getRequesterTicketId());

		validateTicketForTrade(ownerTicket);
		validateTicketForTrade(requesterTicket);

		// 2. 양쪽 티켓을 EXCHANGED 상태로 변경
		ticketService.exchangeTicket(
			ownerTicket.getReservationId(),
			ownerTicket.getMemberId(),
			ownerTicket.getSeatId()
		);
		ticketService.exchangeTicket(
			requesterTicket.getReservationId(),
			requesterTicket.getMemberId(),
			requesterTicket.getSeatId()
		);

		// 3. 신청자에게 소유자의 좌석으로 새 티켓 생성
		ticketService.createTicket(
			ownerTicket.getReservationId(),
			tradeRequest.getRequesterId(),
			ownerTicket.getSeatId()
		);

		// 4. 소유자에게 신청자의 좌석으로 새 티켓 생성
		ticketService.createTicket(
			requesterTicket.getReservationId(),
			trade.getMemberId(),
			requesterTicket.getSeatId()
		);
	}

	private void validateTicketForTrade(Ticket ticket) {
		if (ticket.getStatus() != TicketStatus.ISSUED) {
			throw new BusinessException(TicketErrorCode.TICKET_NOT_TRANSFERABLE);
		}
	}

	@Transactional
	public void rejectTradeRequest(Long tradeRequestId, Long memberId) {
		TradeRequest tradeRequest = findTradeRequestById(tradeRequestId);
		Trade trade = tradeRequest.getTrade();

		validateTradeOwner(trade, memberId);
		validateTradeRequestIsPending(tradeRequest);

		tradeRequest.reject();
	}

	private Trade findTradeById(Long tradeId) {
		return tradeRepository.findById(tradeId)
			.orElseThrow(() -> new BusinessException(TradeErrorCode.TRADE_NOT_FOUND));
	}

	private TradeRequest findTradeRequestById(Long tradeRequestId) {
		return tradeRequestRepository.findById(tradeRequestId)
			.orElseThrow(() -> new BusinessException(TradeErrorCode.TRADE_REQUEST_NOT_FOUND));
	}

	private void validateTradeIsActive(Trade trade) {
		if (trade.getStatus() != TradeStatus.ACTIVE) {
			throw new BusinessException(TradeErrorCode.INVALID_TRADE_STATUS);
		}
	}

	private void validateNotOwnTrade(Trade trade, Long requesterId) {
		if (trade.getMemberId().equals(requesterId)) {
			throw new BusinessException(TradeErrorCode.CANNOT_REQUEST_OWN_TRADE);
		}
	}

	private void validateNoDuplicateRequest(Trade trade, Long requesterId) {
		List<TradeRequest> existingRequests = tradeRequestRepository.findByRequesterIdAndStatus(
			requesterId,
			TradeRequestStatus.PENDING
		);

		boolean hasDuplicate = existingRequests.stream()
			.anyMatch(req -> req.getTrade().getId().equals(trade.getId()));

		if (hasDuplicate) {
			throw new BusinessException(TradeErrorCode.DUPLICATE_TRADE_REQUEST);
		}
	}

	private void validateTradeOwner(Trade trade, Long memberId) {
		if (!trade.getMemberId().equals(memberId)) {
			throw new BusinessException(TradeErrorCode.UNAUTHORIZED_TRADE_REQUEST_ACCESS);
		}
	}

	private void validateTradeRequestIsPending(TradeRequest tradeRequest) {
		if (tradeRequest.getStatus() != TradeRequestStatus.PENDING) {
			throw new BusinessException(TradeErrorCode.INVALID_TRADE_REQUEST_STATUS);
		}
	}

	private void validateNoAcceptedRequests(Trade trade) {
		List<TradeRequest> acceptedRequests = tradeRequestRepository.findByTradeAndStatus(
			trade,
			TradeRequestStatus.ACCEPTED
		);

		if (!acceptedRequests.isEmpty()) {
			throw new BusinessException(TradeErrorCode.TRADE_ALREADY_HAS_ACCEPTED_REQUEST);
		}
	}
}
