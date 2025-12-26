package com.back.b2st.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ConfirmRecoveryReq(
	@NotBlank(message = "복구 토큰이 필요합니다.")
	String token
) {
}
