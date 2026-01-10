package com.back.b2st.domain.email.metrics;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class EmailMetrics {

	private final Counter emailSentSuccessCounter;
	private final Counter emailSentFailureCounter;
	private final Counter verificationSuccessCounter;
	private final Counter verificationFailureCounter;

	public EmailMetrics(MeterRegistry registry) {
		this.emailSentSuccessCounter = Counter.builder("email_sent_total")
			.tag("result", "success")
			.description("이메일 발송 성공 횟수")
			.register(registry);

		this.emailSentFailureCounter = Counter.builder("email_sent_total")
			.tag("result", "failure")
			.description("이메일 발송 실패 횟수")
			.register(registry);

		this.verificationSuccessCounter = Counter.builder("email_verification_total")
			.tag("result", "success")
			.description("이메일 인증 성공 횟수")
			.register(registry);

		this.verificationFailureCounter = Counter.builder("email_verification_total")
			.tag("result", "failure")
			.description("이메일 인증 실패 횟수")
			.register(registry);
	}

	public void recordEmailSent(boolean success) {
		if (success) {
			emailSentSuccessCounter.increment();
		} else {
			emailSentFailureCounter.increment();
		}
	}

	public void recordVerification(boolean success) {
		if (success) {
			verificationSuccessCounter.increment();
		} else {
			verificationFailureCounter.increment();
		}
	}
}
