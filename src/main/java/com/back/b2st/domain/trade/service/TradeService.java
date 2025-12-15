package com.back.b2st.domain.trade.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.back.b2st.domain.trade.repository.TradeRepository;
import com.back.b2st.domain.trade.repository.TradeRequestRepository;
import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.repository.TicketRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
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
	public CreateTradeRes createTrade(CreateTradeReq request, Long memberId) {
		validateTicketNotDuplicated(request.getTicketId());
		validateTradeType(request);

		// Ticket 조회
		Ticket ticket = ticketRepository.findById(request.getTicketId())
			.orElseThrow(() -> new BusinessException(TradeErrorCode.TICKET_NOT_OWNED));

		// 티켓 소유자 검증
		if (!ticket.getMemberId().equals(memberId)) {
			throw new BusinessException(TradeErrorCode.TICKET_NOT_OWNED);
		}

		// Seat 조회
		Seat seat = seatRepository.findById(ticket.getSeatId())
			.orElseThrow(() -> new BusinessException(TradeErrorCode.TICKET_NOT_OWNED));

		// Reservation 조회
		Reservation reservation = reservationRepository.findById(ticket.getReservationId())
			.orElseThrow(() -> new BusinessException(TradeErrorCode.TICKET_NOT_OWNED));

		// 실제 데이터 사용
		String section = seat.getSectionName();
		String row = seat.getRowLabel();
		String seatNumber = seat.getSeatNumber().toString();
		Long performanceId = reservation.getPerformanceId();
		Long scheduleId = 1L;  // 회차 연결 전까지 하드코딩 유지

		Trade trade = Trade.builder()
			.memberId(memberId)
			.performanceId(performanceId)
			.scheduleId(scheduleId)
			.ticketId(request.getTicketId())
			.type(request.getType())
			.price(request.getPrice())
			.totalCount(request.getTotalCount())
			.section(section)
			.row(row)
			.seatNumber(seatNumber)
			.build();

		try {
			Trade savedTrade = tradeRepository.save(trade);
			return new CreateTradeRes(savedTrade);
		} catch (DataIntegrityViolationException e) {
			throw new BusinessException(TradeErrorCode.TICKET_ALREADY_REGISTERED);
		}
	}

	private void validateTicketNotDuplicated(Long ticketId) {
		boolean exists = tradeRepository.existsByTicketIdAndStatus(
			ticketId,
			TradeStatus.ACTIVE
		);

		if (exists) {
			throw new BusinessException(TradeErrorCode.TICKET_ALREADY_REGISTERED);
		}
	}

	@Transactional
	public void updateTrade(Long tradeId, UpdateTradeReq request, Long memberId) {
		Trade trade = findTradeById(tradeId);

		validateTradeOwner(trade, memberId);
		validateTradeIsActive(trade);
		validateTradeIsTransfer(trade);

		trade.updatePrice(request.getPrice());
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
		if (request.getType() == TradeType.EXCHANGE) {
			if (request.getTotalCount() != 1) {
				throw new BusinessException(TradeErrorCode.INVALID_EXCHANGE_COUNT);
			}
			if (request.getPrice() != null) {
				throw new BusinessException(TradeErrorCode.INVALID_EXCHANGE_PRICE);
			}
		} else if (request.getType() == TradeType.TRANSFER) {
			if (request.getPrice() == null || request.getPrice() <= 0) {
				throw new BusinessException(TradeErrorCode.INVALID_TRANSFER_PRICE);
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
			throw new BusinessException(TradeErrorCode.INVALID_TRADE_STATUS);
		}
	}

	private void validateTradeIsTransfer(Trade trade) {
		if (trade.getType() == TradeType.EXCHANGE) {
			throw new BusinessException(TradeErrorCode.CANNOT_UPDATE_EXCHANGE_TRADE);
		}
	}

	private void validateNoPendingRequests(Trade trade) {
		List<TradeRequest> pendingRequests = tradeRequestRepository.findByTradeAndStatus(
			trade,
			TradeRequestStatus.PENDING
		);

		if (!pendingRequests.isEmpty()) {
			throw new BusinessException(TradeErrorCode.CANNOT_DELETE_WITH_PENDING_REQUESTS);
		}
	}
}
