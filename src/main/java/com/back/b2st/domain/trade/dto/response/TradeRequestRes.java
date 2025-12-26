package com.back.b2st.domain.trade.dto.response;

import java.time.LocalDateTime;

import com.back.b2st.domain.trade.entity.TradeRequest;
import com.back.b2st.domain.trade.entity.TradeRequestStatus;

public record
TradeRequestRes(
	Long tradeRequestId,
	Long tradeId,
	Long requesterId,
	Long requesterTicketId,
	TradeRequestStatus status,
	LocalDateTime createdAt,
	LocalDateTime modifiedAt
) {

	public static TradeRequestRes from(TradeRequest tradeRequest) {
		return new TradeRequestRes(
			tradeRequest.getId(),
			tradeRequest.getTrade().getId(),
			tradeRequest.getRequesterId(),
			tradeRequest.getRequesterTicketId(),
			tradeRequest.getStatus(),
			tradeRequest.getCreatedAt(),
			tradeRequest.getModifiedAt()
		);
	}
}
