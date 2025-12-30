package com.back.b2st.domain.prereservation.dto.request;

import jakarta.validation.constraints.NotNull;

public record PrereservationReq(
	@NotNull Long sectionId
) {
}
