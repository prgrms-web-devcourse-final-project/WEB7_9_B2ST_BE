package com.back.b2st.domain.payment.dto.request;

import jakarta.validation.constraints.NotNull;

public record PaymentFailReq(
	@NotNull PaymentFailReason reason
) {
}
