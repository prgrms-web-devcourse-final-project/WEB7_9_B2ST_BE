package com.back.b2st.domain.trade.dto.response;

import java.time.LocalDateTime;

import com.back.b2st.domain.trade.entity.TradeRequest;
import com.back.b2st.domain.trade.entity.TradeRequestStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TradeRequestResponse {

	private Long tradeRequestId;
	private Long tradeId;
	private Long requesterId;
	private Long requesterTicketId;
	private TradeRequestStatus status;
	private LocalDateTime createdAt;
	private LocalDateTime modifiedAt;

	public static TradeRequestResponse from(TradeRequest tradeRequest) {
		return TradeRequestResponse.builder()
			.tradeRequestId(tradeRequest.getId())
			.tradeId(tradeRequest.getTrade().getId())
			.requesterId(tradeRequest.getRequesterId())
			.requesterTicketId(tradeRequest.getRequesterTicketId())
			.status(tradeRequest.getStatus())
			.createdAt(tradeRequest.getCreatedAt())
			.modifiedAt(tradeRequest.getModifiedAt())
			.build();
	}
}