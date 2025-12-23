package com.back.b2st.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PaymentFailReq(
	@NotBlank String reason
) {
}

