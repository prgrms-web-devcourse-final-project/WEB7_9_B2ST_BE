package com.back.b2st.domain.venue.section.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateSectionReq(
	@NotBlank(message = "구역명은 필수입니다.")
	String sectionName
) {
}
