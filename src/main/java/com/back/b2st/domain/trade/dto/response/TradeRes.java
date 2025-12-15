package com.back.b2st.domain.trade.dto.response;

import java.time.LocalDateTime;

import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.entity.TradeType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TradeRes {

	private Long tradeId;
	private Long memberId;
	private Long performanceId;
	private Long scheduleId;
	private Long ticketId;
	private TradeType type;
	private TradeStatus status;
	private Integer price;
	private Integer totalCount;
	private String section;
	private String row;
	private String seatNumber;
	private LocalDateTime createdAt;

	public static TradeRes from(Trade trade) {
		return TradeRes.builder()
			.tradeId(trade.getId())
			.memberId(trade.getMemberId())
			.performanceId(trade.getPerformanceId())
			.scheduleId(trade.getScheduleId())
			.ticketId(trade.getTicketId())
			.type(trade.getType())
			.status(trade.getStatus())
			.price(trade.getPrice())
			.totalCount(trade.getTotalCount())
			.section(trade.getSection())
			.row(trade.getRow())
			.seatNumber(trade.getSeatNumber())
			.createdAt(trade.getCreatedAt())
			.build();
	}
}

