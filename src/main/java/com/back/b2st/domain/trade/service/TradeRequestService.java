package com.back.b2st.domain.trade.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.notification.event.NotificationEmailEvent;
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
import com.back.b2st.domain.trade.mapper.TradeRequestMapper;
import com.back.b2st.domain.trade.repository.TradeRepository;
import com.back.b2st.domain.trade.repository.TradeRequestRepository;
import com.back.b2st.global.error.exception.BusinessException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeRequestService {

	private final TradeRequestRepository tradeRequestRepository;
	private final TradeRepository tradeRepository;
	private final TicketService ticketService;
	private final ApplicationEventPublisher eventPublisher;

	@PersistenceContext
	private EntityManager entityManager;

	@Transactional
	public TradeRequestRes createTradeRequest(Long tradeId, CreateTradeRequestReq request,
		Long requesterId) {
		Trade trade = findTradeById(tradeId);

		validateTradeIsActive(trade);
		validateNotOwnTrade(trade, requesterId);
		validateOnlyExchangeAllowed(trade); // TRANSFER는 신청 불가, 바로 결제
		validateNoDuplicateRequest(trade, requesterId);

		TradeRequest tradeRequest = TradeRequestMapper.toEntity(request, trade, requesterId);

		TradeRequest savedRequest = tradeRequestRepository.save(tradeRequest);
		eventPublisher.publishEvent(
			NotificationEmailEvent.exchangeRequested(trade.getMemberId(), trade.getPerformanceId())
		);
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
		TradeRequest tradeRequest = findTradeRequestByIdWithLock(tradeRequestId);
		Trade trade = findTradeByIdWithLock(tradeRequest.getTrade().getId());

		validateTradeOwner(trade, memberId);
		validateTradeIsActive(trade);
		validateTradeRequestIsPending(tradeRequest);
		validateNoAcceptedRequests(trade);

		// EXCHANGE만 처리 (TRANSFER는 바로 결제)
		if (trade.getType() != TradeType.EXCHANGE) {
			throw new BusinessException(TradeErrorCode.INVALID_REQUEST,
				"양도(TRANSFER)는 신청/수락이 아닌 즉시 결제 방식입니다.");
		}

		tradeRequest.accept();
		trade.complete();

		// 교환: 양쪽 티켓을 서로 교환
		handleExchange(trade, tradeRequest);

		eventPublisher.publishEvent(
			NotificationEmailEvent.exchangeAccepted(tradeRequest.getRequesterId(), trade.getPerformanceId())
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
		TradeRequest tradeRequest = findTradeRequestByIdWithLock(tradeRequestId);
		Trade trade = findTradeByIdWithLock(tradeRequest.getTrade().getId());

		validateTradeOwner(trade, memberId);
		validateTradeRequestIsPending(tradeRequest);

		tradeRequest.reject();

		eventPublisher.publishEvent(
			NotificationEmailEvent.exchangeRejected(tradeRequest.getRequesterId(), trade.getPerformanceId())
		);
	}

	private Trade findTradeById(Long tradeId) {
		return tradeRepository.findById(tradeId)
			.orElseThrow(() -> new BusinessException(TradeErrorCode.TRADE_NOT_FOUND));
	}

	private Trade findTradeByIdWithLock(Long tradeId) {
		Trade trade = entityManager.find(Trade.class, tradeId, LockModeType.PESSIMISTIC_WRITE);
		if (trade == null) {
			throw new BusinessException(TradeErrorCode.TRADE_NOT_FOUND);
		}
		return trade;
	}

	private TradeRequest findTradeRequestById(Long tradeRequestId) {
		return tradeRequestRepository.findById(tradeRequestId)
			.orElseThrow(() -> new BusinessException(TradeErrorCode.TRADE_REQUEST_NOT_FOUND));
	}

	private TradeRequest findTradeRequestByIdWithLock(Long tradeRequestId) {
		TradeRequest tradeRequest = entityManager.find(TradeRequest.class, tradeRequestId,
			LockModeType.PESSIMISTIC_WRITE);
		if (tradeRequest == null) {
			throw new BusinessException(TradeErrorCode.TRADE_REQUEST_NOT_FOUND);
		}
		return tradeRequest;
	}

	private void validateTradeIsActive(Trade trade) {
		if (trade.getStatus() != TradeStatus.ACTIVE) {
			throw new BusinessException(TradeErrorCode.INVALID_REQUEST, "유효하지 않은 거래 상태입니다.");
		}
	}

	private void validateNotOwnTrade(Trade trade, Long requesterId) {
		if (trade.getMemberId().equals(requesterId)) {
			throw new BusinessException(TradeErrorCode.INVALID_REQUEST, "자신의 게시글에는 신청할 수 없습니다.");
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
			throw new BusinessException(TradeErrorCode.INVALID_REQUEST, "이미 신청한 게시글입니다.");
		}
	}

	private void validateTradeOwner(Trade trade, Long memberId) {
		if (!trade.getMemberId().equals(memberId)) {
			throw new BusinessException(TradeErrorCode.UNAUTHORIZED_TRADE_REQUEST_ACCESS);
		}
	}

	private void validateTradeRequestIsPending(TradeRequest tradeRequest) {
		if (tradeRequest.getStatus() != TradeRequestStatus.PENDING) {
			throw new BusinessException(TradeErrorCode.INVALID_REQUEST, "유효하지 않은 교환 신청 상태입니다.");
		}
	}

	private void validateNoAcceptedRequests(Trade trade) {
		List<TradeRequest> acceptedRequests = tradeRequestRepository.findByTradeAndStatus(
			trade,
			TradeRequestStatus.ACCEPTED
		);

		if (!acceptedRequests.isEmpty()) {
			throw new BusinessException(TradeErrorCode.INVALID_REQUEST, "이미 수락된 신청이 있습니다.");
		}
	}

	private void validateOnlyExchangeAllowed(Trade trade) {
		if (trade.getType() == TradeType.TRANSFER) {
			throw new BusinessException(TradeErrorCode.INVALID_REQUEST,
				"양도(TRANSFER)는 신청 없이 바로 결제해주세요.");
		}
	}
}
