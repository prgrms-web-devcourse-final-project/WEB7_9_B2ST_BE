package com.back.b2st.domain.venue.section.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateSectionReq(
	@NotNull(message = "구역명은 필수입니다.")
	String sectionName
) {
}
