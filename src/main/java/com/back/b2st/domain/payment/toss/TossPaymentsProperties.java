package com.back.b2st.domain.payment.toss;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toss.payments")
public record TossPaymentsProperties(
	String baseUrl,
	String secretKey
) {
	public TossPaymentsProperties {
		if (baseUrl == null || baseUrl.isBlank()) {
			baseUrl = "https://api.tosspayments.com";
		}
	}
}

