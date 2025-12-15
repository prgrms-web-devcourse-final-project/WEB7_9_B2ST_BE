package com.back.b2st.domain.trade.dto.response;

import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.entity.TradeType;

import lombok.Getter;

@Getter
public class CreateTradeRes {

	private final Long tradeId;
	private final TradeType type;
	private final TradeStatus status;
	private final String section;
	private final String row;
	private final String seatNumber;
	private final Integer totalCount;
	private final Integer price;

	public CreateTradeRes(Trade trade) {
		this.tradeId = trade.getId();
		this.type = trade.getType();
		this.status = trade.getStatus();
		this.section = trade.getSection();
		this.row = trade.getRow();
		this.seatNumber = trade.getSeatNumber();
		this.totalCount = trade.getTotalCount();
		this.price = trade.getPrice();
	}
}
