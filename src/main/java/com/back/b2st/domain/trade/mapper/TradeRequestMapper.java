package com.back.b2st.domain.trade.mapper;

import com.back.b2st.domain.trade.dto.request.CreateTradeRequestReq;
import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeRequest;

public class TradeRequestMapper {

	public static TradeRequest toEntity(
		CreateTradeRequestReq request,
		Trade trade,
		Long requesterId
	) {
		return TradeRequest.builder()
			.trade(trade)
			.requesterId(requesterId)
			.requesterTicketId(request.requesterTicketId())
			.build();
	}
}
