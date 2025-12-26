package com.back.b2st.domain.payment.dto.request;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.PaymentMethod;

import jakarta.validation.constraints.NotNull;

public record PaymentPrepareReq(
	@NotNull DomainType domainType,
	@NotNull Long domainId,
	@NotNull PaymentMethod paymentMethod
) {
}

