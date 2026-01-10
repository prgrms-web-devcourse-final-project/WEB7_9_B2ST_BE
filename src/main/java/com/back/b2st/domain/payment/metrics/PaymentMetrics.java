package com.back.b2st.domain.payment.metrics;

import org.springframework.stereotype.Component;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.PaymentMethod;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class PaymentMetrics {
	private final MeterRegistry registry;
	private final Timer paymentProcessTimer;
	private final DistributionSummary paymentAmountSummary;

	public PaymentMetrics(MeterRegistry registry) {
		this.registry = registry;

		this.paymentProcessTimer = Timer.builder("payment_process_duration")
				.description("결제 처리 소요 시간")
				.publishPercentiles(0.5, 0.95, 0.99)
				.register(registry);

		this.paymentAmountSummary = DistributionSummary.builder("payment_amount")
				.description("결제 금액 분포")
				.baseUnit("won")
				.publishPercentiles(0.5, 0.95, 0.99)
				.register(registry);
	}

	/** 결제 성공 기록 */
	public void recordPaymentSuccess(DomainType domainType, PaymentMethod method, int amount) {
		Counter.builder("payment_total")
				.tag("result", "success")
				.tag("domain_type", domainType.name())
				.tag("method", method.name())
				.register(registry)
				.increment();
		paymentAmountSummary.record(amount);
	}

	/** 결제 실패 기록 */
	public void recordPaymentFailure(DomainType domainType, String reason) {
		Counter.builder("payment_total")
				.tag("result", "failure")
				.tag("domain_type", domainType.name())
				.tag("reason", reason)
				.register(registry)
				.increment();
	}

	/** 환불 처리 기록 */
	public void recordRefund(DomainType domainType, int amount) {
		Counter.builder("payment_refund_total")
				.tag("domain_type", domainType.name())
				.register(registry)
				.increment();
		Counter.builder("payment_refund_amount_total")
				.tag("domain_type", domainType.name())
				.register(registry)
				.increment(amount);
	}

	/** 결제 처리 시간 측정 시작 */
	public Timer.Sample startPaymentTimer() {
		return Timer.start(registry);
	}

	/** 결제 처리 시간 측정 종료 */
	public void stopPaymentTimer(Timer.Sample sample) {
		sample.stop(paymentProcessTimer);
	}
}
