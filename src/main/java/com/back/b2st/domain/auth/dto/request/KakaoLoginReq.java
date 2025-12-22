package com.back.b2st.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginReq(
	@NotBlank(message = "인가 코드는 필수입니다.")
	String code,
	String state
) {
}
