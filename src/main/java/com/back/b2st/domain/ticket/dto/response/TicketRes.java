package com.back.b2st.domain.ticket.dto.response;

import com.back.b2st.domain.ticket.entity.TicketStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TicketRes {

	private Long ticketId;
	private Long reservationId;
	private Long seatId;
	private TicketStatus status;
	private String sectionName;
	private String rowLabel;
	private Integer seatNumber;
	private Long performanceId;
}
