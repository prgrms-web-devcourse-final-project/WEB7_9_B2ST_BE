package com.back.b2st.domain.seat.seat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CreateSeatReq(
	@NotNull(message = "구역 정보는 필수입니다.")
	Long sectionId,

	@NotBlank(message = "열 정보는 필수입니다.")
	@Pattern(
		regexp = "^[a-zA-Z0-9가-힣]{1,5}$",
		message = "숫자, 영문, 한글만 입력 가능하며 5자 이하여야 합니다."
	)
	String rowLabel,

	@NotNull(message = "좌석 번호는 필수입니다.")
	@Positive(message = "좌석 번호는 1 이상이어야 합니다.")
	Integer seatNumber
) {
}
