package com.back.b2st.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TokenReissueReq(
	@NotBlank(message = "Access Token은 필수입니다.")
	String accessToken,

	@NotBlank(message = "Refresh Token은 필수입니다.")
	String refreshToken
) {
}
