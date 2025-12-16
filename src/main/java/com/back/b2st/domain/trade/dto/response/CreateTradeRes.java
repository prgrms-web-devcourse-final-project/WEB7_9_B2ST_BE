package com.back.b2st.domain.trade.dto.response;

import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.entity.TradeType;

public record CreateTradeRes(
	Long tradeId,
	TradeType type,
	TradeStatus status,
	String section,
	String row,
	String seatNumber,
	Integer totalCount,
	Integer price
) {

	public static CreateTradeRes from(Trade trade) {
		return new CreateTradeRes(
			trade.getId(),
			trade.getType(),
			trade.getStatus(),
			trade.getSection(),
			trade.getRow(),
			trade.getSeatNumber(),
			trade.getTotalCount(),
			trade.getPrice()
		);
	}
}
