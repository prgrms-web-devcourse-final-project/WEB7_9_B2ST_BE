package com.back.b2st.domain.payment.toss;

import java.time.LocalDateTime;

public record TossPaymentsConfirmResult(
	String paymentKey,
	String orderId,
	Long amount,
	LocalDateTime approvedAt
) {
}

