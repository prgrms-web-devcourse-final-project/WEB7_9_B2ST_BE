package com.back.b2st.domain.payment.service;

import com.back.b2st.domain.payment.entity.DomainType;

public interface PaymentDomainHandler {
	boolean supports(DomainType domainType);

	PaymentTarget loadAndValidate(Long domainId, Long memberId);
}

