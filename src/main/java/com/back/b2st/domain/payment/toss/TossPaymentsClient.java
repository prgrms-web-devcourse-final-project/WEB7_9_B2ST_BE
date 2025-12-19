package com.back.b2st.domain.payment.toss;

public interface TossPaymentsClient {
	TossPaymentsConfirmResult confirm(String paymentKey, String orderId, Long amount);
}

