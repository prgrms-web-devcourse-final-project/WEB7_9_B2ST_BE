package com.back.b2st.domain.seat.grade.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateSeatGradeReq(
	@NotNull(message = "좌석id는 필수 입니다.")
	Long seatId,
	String grade,
	@NotNull(message = "가격은 필수 입니다.")
	@Min(value = 1, message = "가격은 1 이상이어야 합니다.")
	Integer price
) {
}
