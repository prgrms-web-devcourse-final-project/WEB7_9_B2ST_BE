package com.back.b2st.domain.payment.service;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;

/**
 * 결제 실패 시 도메인별 후처리를 담당하는 핸들러 인터페이스
 */
public interface PaymentFailureHandler {

	boolean supports(DomainType domainType);

	void handleFailure(Payment payment);
}

