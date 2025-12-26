package com.back.b2st.domain.payment.service;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;

public interface PaymentFinalizer {
	boolean supports(DomainType domainType);

	void finalizePayment(Payment payment);
}

