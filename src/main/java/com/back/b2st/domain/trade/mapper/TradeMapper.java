package com.back.b2st.domain.trade.mapper;

import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.trade.dto.request.CreateTradeReq;
import com.back.b2st.domain.trade.entity.Trade;

public class TradeMapper {

	public static Trade toEntity(
		CreateTradeReq request,
		Ticket ticket,
		Seat seat,
		Reservation reservation,
		Long memberId
	) {
		return Trade.builder()
			.memberId(memberId)
			.performanceId(reservation.getScheduleId())
			.scheduleId(1L)  // 회차 연결 전까지 하드코딩 유지
			.ticketId(ticket.getId())
			.type(request.type())
			.price(request.price())
			.totalCount(request.ticketIds().size())
			.section(seat.getSectionName())
			.row(seat.getRowLabel())
			.seatNumber(seat.getSeatNumber().toString())
			.build();
	}
}
