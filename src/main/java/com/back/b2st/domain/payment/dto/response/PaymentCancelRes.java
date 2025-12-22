package com.back.b2st.domain.payment.dto.response;

import java.time.LocalDateTime;

import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentStatus;

public record PaymentCancelRes(
	String orderId,
	Long amount,
	PaymentStatus status,
	LocalDateTime canceledAt,
	String failureReason
) {
	public static PaymentCancelRes from(Payment payment) {
		return new PaymentCancelRes(
			payment.getOrderId(),
			payment.getAmount(),
			payment.getStatus(),
			payment.getCanceledAt(),
			payment.getFailureReason()
		);
	}
}
