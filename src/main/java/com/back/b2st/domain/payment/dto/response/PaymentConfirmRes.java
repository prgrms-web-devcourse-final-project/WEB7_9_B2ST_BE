package com.back.b2st.domain.payment.dto.response;

import java.time.LocalDateTime;

import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentStatus;

public record PaymentConfirmRes(
	Long paymentId,
	String orderId,
	Long amount,
	PaymentStatus status,
	LocalDateTime paidAt
) {

	public static PaymentConfirmRes from(Payment payment) {
		return new PaymentConfirmRes(
			payment.getId(),
			payment.getOrderId(),
			payment.getAmount(),
			payment.getStatus(),
			payment.getPaidAt()
		);
	}
}
