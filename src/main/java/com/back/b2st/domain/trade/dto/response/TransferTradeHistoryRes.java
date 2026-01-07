package com.back.b2st.domain.trade.dto.response;

import java.time.LocalDateTime;

import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.entity.TradeType;

public record TransferTradeHistoryRes(
	Long tradeId,
	TradeType type,
	TradeStatus status,
	Integer price,
	Long performanceId,
	Long scheduleId,
	Integer totalCount,
	String section,
	String row,
	String seatNumber,
	LocalDateTime purchasedAt,
	LocalDateTime createdAt
) {

	public static TransferTradeHistoryRes from(Trade trade) {
		return new TransferTradeHistoryRes(
			trade.getId(),
			trade.getType(),
			trade.getStatus(),
			trade.getPrice(),
			trade.getPerformanceId(),
			trade.getScheduleId(),
			trade.getTotalCount(),
			trade.getSection(),
			trade.getRow(),
			trade.getSeatNumber(),
			trade.getPurchasedAt(),
			trade.getCreatedAt()
		);
	}
}

