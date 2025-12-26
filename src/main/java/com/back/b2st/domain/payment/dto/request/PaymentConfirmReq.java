package com.back.b2st.domain.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record PaymentConfirmReq(
	@NotBlank String orderId,
	@NotNull Long amount
) {
}
