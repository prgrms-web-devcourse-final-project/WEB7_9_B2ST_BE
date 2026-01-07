package com.back.b2st.domain.trade.dto.response;

import java.time.LocalDateTime;

import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.entity.TradeType;

public record TradeRes(
	Long tradeId,
	Long memberId,
	Long performanceId,
	String performanceTitle,  // 공연명 추가
	Long scheduleId,
	Long ticketId,
	TradeType type,
	TradeStatus status,
	Integer price,
	Integer totalCount,
	String section,
	String row,
	String seatNumber,
	LocalDateTime createdAt
) {

	public static TradeRes from(Trade trade, String performanceTitle) {
		return new TradeRes(
			trade.getId(),
			trade.getMemberId(),
			trade.getPerformanceId(),
			performanceTitle,  // 공연명 포함
			trade.getScheduleId(),
			trade.getTicketId(),
			trade.getType(),
			trade.getStatus(),
			trade.getPrice(),
			trade.getTotalCount(),
			trade.getSection(),
			trade.getRow(),
			trade.getSeatNumber(),
			trade.getCreatedAt()
		);
	}
}
