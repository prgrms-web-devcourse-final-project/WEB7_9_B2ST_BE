package com.back.b2st.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentConfirmReq(
	@NotBlank String orderId,
	@NotNull Long amount
) {
}
