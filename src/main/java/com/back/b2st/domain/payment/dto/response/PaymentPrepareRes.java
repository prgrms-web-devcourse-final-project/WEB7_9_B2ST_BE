package com.back.b2st.domain.payment.dto.response;

import java.time.LocalDateTime;

import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentStatus;

public record PaymentPrepareRes(
	Long paymentId,
	String orderId,
	Long amount,
	PaymentStatus status,
	LocalDateTime expiresAt
) {
	public static PaymentPrepareRes from(Payment payment) {
		return new PaymentPrepareRes(
			payment.getId(),
			payment.getOrderId(),
			payment.getAmount(),
			payment.getStatus(),
			payment.getExpiresAt()
		);
	}
}
