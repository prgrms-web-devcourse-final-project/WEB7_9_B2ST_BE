package com.back.b2st.domain.payment.service;

import com.back.b2st.domain.payment.entity.DomainType;

public record PaymentTarget(
	DomainType domainType,
	Long domainId,
	Long expectedAmount
) {
}

