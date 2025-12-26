package com.back.b2st.domain.scheduleseat.dto.response;

import com.back.b2st.domain.scheduleseat.entity.SeatStatus;

public record ScheduleSeatViewRes(

	Long scheduleSeatId,
	Long seatId,

	String sectionName,
	String rowLabel,
	Integer seatNumber,

	SeatStatus status,

	String grade,
	Integer price
) {
}
