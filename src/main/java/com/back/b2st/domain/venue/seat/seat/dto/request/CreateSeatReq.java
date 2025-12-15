package com.back.b2st.domain.venue.seat.seat.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateSeatReq(
	@NotNull(message = "구역 정보는 필수입니다.")
	Long sectionId,

	@NotNull(message = "열 정보는 필수입니다.")
	String rowLabel,

	@NotNull(message = "좌석 번호는 필수입니다.")
	Integer seatNumber
) {
}
