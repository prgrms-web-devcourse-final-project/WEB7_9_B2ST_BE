package com.back.b2st.domain.trade.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.trade.dto.request.CreateTradeRequest;
import com.back.b2st.domain.trade.dto.response.CreateTradeResponse;
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

	@Transactional
	public CreateTradeResponse createTrade(CreateTradeRequest request, Long memberId) {
		// 1. 티켓 중복 등록 검증
		validateTicketNotDuplicated(request.getTicketId());

		// 2. 교환/양도 타입별 검증
		validateTradeType(request);

		// 3. TODO: Ticket 도메인에서 티켓 정보 조회
		// Ticket ticket = ticketService.getTicket(request.getTicketId());
		// validateTicketOwnership(ticket, memberId);
		// String section = ticket.getSection();
		// String row = ticket.getRow();
		// String seatNumber = ticket.getSeatNumber();
		// Long performanceId = ticket.getPerformanceId();
		// Long scheduleId = ticket.getScheduleId();

		// 임시 Mock 데이터 (Ticket 연동 전)
		String section = "A";
		String row = "5열";
		String seatNumber = "12석";
		Long performanceId = 1L;  // Mock
		Long scheduleId = 1L;     // Mock

		// 4. Trade 생성
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

		Trade savedTrade = tradeRepository.save(trade);

		return new CreateTradeResponse(savedTrade);
	}

	// 티켓 중복 등록 검증
	private void validateTicketNotDuplicated(Long ticketId) {
		boolean exists = tradeRepository.existsByTicketIdAndStatus(
			ticketId,
			TradeStatus.ACTIVE
		);

		if (exists) {
			throw new BusinessException(TradeErrorCode.TICKET_ALREADY_REGISTERED);
		}
	}

	// 교환/양도 타입별 검증
	private void validateTradeType(CreateTradeRequest request) {
		if (request.getType() == TradeType.EXCHANGE) {
			// 교환: totalCount = 1, price = null
			if (request.getTotalCount() != 1) {
				throw new BusinessException(TradeErrorCode.INVALID_EXCHANGE_COUNT);
			}
			if (request.getPrice() != null) {
				throw new BusinessException(TradeErrorCode.INVALID_EXCHANGE_PRICE);
			}
		} else if (request.getType() == TradeType.TRANSFER) {
			// 양도: price 필수
			if (request.getPrice() == null || request.getPrice() <= 0) {
				throw new BusinessException(TradeErrorCode.INVALID_TRANSFER_PRICE);
			}
		}
	}
}
