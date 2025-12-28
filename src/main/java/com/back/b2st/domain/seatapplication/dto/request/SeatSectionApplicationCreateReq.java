package com.back.b2st.domain.seatapplication.dto.request;

import jakarta.validation.constraints.NotNull;

public record SeatSectionApplicationCreateReq(
	@NotNull Long sectionId
) {
}
