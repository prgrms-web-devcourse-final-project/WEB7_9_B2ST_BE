package com.back.b2st.domain.trade.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.repository.TicketRepository;
import com.back.b2st.domain.trade.dto.request.CreateTradeReq;
import com.back.b2st.domain.trade.dto.request.UpdateTradeReq;
import com.back.b2st.domain.trade.dto.response.CreateTradeRes;
import com.back.b2st.domain.trade.dto.response.TradeRes;
import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeRequest;
import com.back.b2st.domain.trade.entity.TradeRequestStatus;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.entity.TradeType;
import com.back.b2st.domain.trade.error.TradeErrorCode;
import com.back.b2st.domain.trade.mapper.TradeMapper;
import com.back.b2st.domain.trade.repository.TradeRepository;
import com.back.b2st.domain.trade.repository.TradeRequestRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeService {

	private final TradeRepository tradeRepository;
	private final TradeRequestRepository tradeRequestRepository;
	private final TicketRepository ticketRepository;
	private final SeatRepository seatRepository;
	private final ReservationRepository reservationRepository;

	public TradeRes getTrade(Long tradeId) {
		Trade trade = tradeRepository.findById(tradeId)
			.orElseThrow(() -> new BusinessException(TradeErrorCode.TRADE_NOT_FOUND));

		return TradeRes.from(trade);
	}

	public Page<TradeRes> getTrades(TradeType type, TradeStatus status, Pageable pageable) {
		Page<Trade> trades;

		if (type != null && status != null) {
			trades = tradeRepository.findAllByTypeAndStatus(type, status, pageable);
		} else if (type != null) {
			trades = tradeRepository.findAllByType(type, pageable);
		} else if (status != null) {
			trades = tradeRepository.findAllByStatus(status, pageable);
		} else {
			trades = tradeRepository.findAll(pageable);
		}

		return trades.map(TradeRes::from);
	}

	@Transactional
	public List<CreateTradeRes> createTrade(CreateTradeReq request, Long memberId) {
		// 교환은 1개만 가능
		if (request.type() == TradeType.EXCHANGE && request.ticketIds().size() != 1) {
			throw new BusinessException(TradeErrorCode.INVALID_REQUEST, "교환은 1개만 가능합니다.");
		}

		// 양도는 1개 이상 가능
		if (request.type() == TradeType.TRANSFER && request.ticketIds().isEmpty()) {
			throw new BusinessException(TradeErrorCode.INVALID_REQUEST, "티켓은 최소 1개 이상이어야 합니다.");
		}

		validateTradeType(request);

		List<CreateTradeRes> results = new java.util.ArrayList<>();

		for (Long ticketId : request.ticketIds()) {
			validateTicketNotDuplicated(ticketId);

			Ticket ticket = ticketRepository.findById(ticketId)
				.orElseThrow(() -> new BusinessException(TradeErrorCode.INVALID_REQUEST, "보유하지 않은 티켓입니다."));

			if (!ticket.getMemberId().equals(memberId)) {
				throw new BusinessException(TradeErrorCode.INVALID_REQUEST, "보유하지 않은 티켓입니다.");
			}

			Seat seat = seatRepository.findById(ticket.getSeatId())
				.orElseThrow(() -> new BusinessException(TradeErrorCode.INVALID_REQUEST, "보유하지 않은 티켓입니다."));

			Reservation reservation = reservationRepository.findById(ticket.getReservationId())
				.orElseThrow(() -> new BusinessException(TradeErrorCode.INVALID_REQUEST, "보유하지 않은 티켓입니다."));

		Trade trade = TradeMapper.toEntity(request, ticket, seat, reservation, memberId);

			try {
				Trade savedTrade = tradeRepository.save(trade);
				results.add(CreateTradeRes.from(savedTrade));
			} catch (DataIntegrityViolationException e) {
				throw new BusinessException(TradeErrorCode.INVALID_REQUEST, "이미 등록된 티켓입니다.");
			}
		}

		return results;
	}

	private void validateTicketNotDuplicated(Long ticketId) {
		boolean exists = tradeRepository.existsByTicketIdAndStatus(
			ticketId,
			TradeStatus.ACTIVE
		);

		if (exists) {
			throw new BusinessException(TradeErrorCode.INVALID_REQUEST, "이미 등록된 티켓입니다.");
		}
	}

	@Transactional
	public void updateTrade(Long tradeId, UpdateTradeReq request, Long memberId) {
		Trade trade = findTradeById(tradeId);

		validateTradeOwner(trade, memberId);
		validateTradeIsActive(trade);
		validateTradeIsTransfer(trade);

		trade.updatePrice(request.price());
	}

	@Transactional
	public void deleteTrade(Long tradeId, Long memberId) {
		Trade trade = findTradeById(tradeId);

		validateTradeOwner(trade, memberId);
		validateTradeIsActive(trade);
		validateNoPendingRequests(trade);

		trade.cancel();
	}

	private Trade findTradeById(Long tradeId) {
		return tradeRepository.findById(tradeId)
			.orElseThrow(() -> new BusinessException(TradeErrorCode.TRADE_NOT_FOUND));
	}

	private void validateTradeType(CreateTradeReq request) {
		if (request.type() == TradeType.EXCHANGE) {
			if (request.price() != null) {
				throw new BusinessException(TradeErrorCode.INVALID_REQUEST, "교환은 가격을 설정할 수 없습니다.");
			}
		} else if (request.type() == TradeType.TRANSFER) {
			if (request.price() == null || request.price() <= 0) {
				throw new BusinessException(TradeErrorCode.INVALID_REQUEST, "양도 가격은 필수입니다.");
			}
		}
	}

	private void validateTradeOwner(Trade trade, Long memberId) {
		if (!trade.getMemberId().equals(memberId)) {
			throw new BusinessException(TradeErrorCode.UNAUTHORIZED_TRADE_ACCESS);
		}
	}

	private void validateTradeIsActive(Trade trade) {
		if (trade.getStatus() != TradeStatus.ACTIVE) {
			throw new BusinessException(TradeErrorCode.INVALID_REQUEST, "유효하지 않은 거래 상태입니다.");
		}
	}

	private void validateTradeIsTransfer(Trade trade) {
		if (trade.getType() == TradeType.EXCHANGE) {
			throw new BusinessException(TradeErrorCode.INVALID_REQUEST, "교환 게시글은 수정할 수 없습니다.");
		}
	}

	private void validateNoPendingRequests(Trade trade) {
		List<TradeRequest> pendingRequests = tradeRequestRepository.findByTradeAndStatus(
			trade,
			TradeRequestStatus.PENDING
		);

		if (!pendingRequests.isEmpty()) {
			throw new BusinessException(TradeErrorCode.INVALID_REQUEST, "대기 중인 교환 신청이 있어 삭제할 수 없습니다.");
		}
	}
}
