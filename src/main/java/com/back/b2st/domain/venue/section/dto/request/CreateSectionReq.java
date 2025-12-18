package com.back.b2st.domain.venue.section.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSectionReq(
	@NotBlank(message = "구역명은 필수입니다.")
	@Size(max = 20, message = "구역명은 20자 이하여야 합니다.")
	String sectionName
) {
}
