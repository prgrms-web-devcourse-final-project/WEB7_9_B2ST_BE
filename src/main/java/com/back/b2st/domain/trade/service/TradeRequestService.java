package com.back.b2st.domain.trade.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.trade.dto.request.CreateTradeRequestRequest;
import com.back.b2st.domain.trade.dto.response.TradeRequestResponse;
import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeRequest;
import com.back.b2st.domain.trade.entity.TradeRequestStatus;
import com.back.b2st.domain.trade.entity.TradeStatus;
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

	@Transactional
	public TradeRequestResponse createTradeRequest(Long tradeId, CreateTradeRequestRequest request,
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
		return TradeRequestResponse.from(savedRequest);
	}

	public TradeRequestResponse getTradeRequest(Long tradeRequestId) {
		TradeRequest tradeRequest = findTradeRequestById(tradeRequestId);
		return TradeRequestResponse.from(tradeRequest);
	}

	public List<TradeRequestResponse> getTradeRequestsByTrade(Long tradeId) {
		Trade trade = findTradeById(tradeId);
		List<TradeRequest> requests = tradeRequestRepository.findByTrade(trade);
		return requests.stream()
			.map(TradeRequestResponse::from)
			.collect(Collectors.toList());
	}

	public List<TradeRequestResponse> getTradeRequestsByRequester(Long requesterId) {
		List<TradeRequest> requests = tradeRequestRepository.findByRequesterId(requesterId);
		return requests.stream()
			.map(TradeRequestResponse::from)
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