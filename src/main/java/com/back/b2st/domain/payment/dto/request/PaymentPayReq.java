package com.back.b2st.domain.payment.dto.request;

import java.util.UUID;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.PaymentMethod;

import jakarta.validation.constraints.NotNull;

public record PaymentPayReq(
	@NotNull DomainType domainType,
	@NotNull PaymentMethod paymentMethod,
	Long domainId,
	UUID entryId
) {
}

