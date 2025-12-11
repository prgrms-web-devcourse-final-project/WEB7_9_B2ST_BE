package com.back.b2st.domain.trade.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.trade.dto.request.CreateTradeRequest;
import com.back.b2st.domain.trade.dto.response.CreateTradeResponse;
import com.back.b2st.domain.trade.dto.response.TradeResponse;
import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.entity.TradeType;
import com.back.b2st.domain.trade.error.TradeErrorCode;
import com.back.b2st.domain.trade.repository.TradeRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeService {

	private final TradeRepository tradeRepository;

	public TradeResponse getTrade(Long tradeId) {
		Trade trade = tradeRepository.findById(tradeId)
			.orElseThrow(() -> new BusinessException(TradeErrorCode.TRADE_NOT_FOUND));

		return TradeResponse.from(trade);
	}

	public Page<TradeResponse> getTrades(TradeType type, TradeStatus status, Pageable pageable) {
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

		return trades.map(TradeResponse::from);
	}

	@Transactional
	public CreateTradeResponse createTrade(CreateTradeRequest request, Long memberId) {
		validateTicketNotDuplicated(request.getTicketId());
		validateTradeType(request);

		String section = "A";
		String row = "5열";
		String seatNumber = "12석";
		Long performanceId = 1L;
		Long scheduleId = 1L;

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
			return new CreateTradeResponse(savedTrade);
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

	private void validateTradeType(CreateTradeRequest request) {
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
}
