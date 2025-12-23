package com.back.b2st.domain.payment.dto.response;

import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentStatus;

public record PaymentFailRes(
	String orderId,
	Long amount,
	PaymentStatus status,
	String failureReason
) {
	public static PaymentFailRes from(Payment payment) {
		return new PaymentFailRes(
			payment.getOrderId(),
			payment.getAmount(),
			payment.getStatus(),
			payment.getFailureReason()
		);
	}
}

