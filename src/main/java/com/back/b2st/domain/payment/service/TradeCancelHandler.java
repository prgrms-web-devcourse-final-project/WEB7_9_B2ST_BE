package com.back.b2st.domain.payment.service;

import org.springframework.stereotype.Component;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

@Component
public class TradeCancelHandler implements PaymentCancelHandler {

	@Override
	public boolean supports(DomainType domainType) {
		return domainType == DomainType.TRADE;
	}

	@Override
	public void handleCancel(Payment payment) {
		throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE,
			"티켓 거래 결제는 취소/환불을 지원하지 않습니다.");
	}
}
