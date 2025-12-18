package com.back.b2st.domain.seat.seat.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSeatReq(
	@NotNull(message = "구역 정보는 필수입니다.")
	Long sectionId,

	@NotBlank(message = "열 정보는 필수입니다.")
	// todo 추가 검증 필요, 공백 특수문자 등, 영문과 숫자만 가능
	String rowLabel,

	@NotNull(message = "좌석 번호는 필수입니다.")
	@Min(value = 1, message = "좌석 번호는 1 이상이어야 합니다.")
	Integer seatNumber
) {
}
