package com.back.b2st.domain.auth.metrics;

import org.springframework.stereotype.Component;

import com.back.b2st.domain.auth.dto.response.SecurityThreatRes;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class SecurityMetrics {

	private final MeterRegistry registry;

	public SecurityMetrics(MeterRegistry registry) {
		this.registry = registry;
	}

	public void recordSecurityThreat(SecurityThreatRes threat) {
		Counter.builder("security_threat_detected_total")
			.tag("type", threat.threatType().name())
			.tag("severity", threat.severity().name())
			.description("보안 위협 탐지 횟수")
			.register(registry)
			.increment();
	}

	public void recordRateLimitTriggered(String endpoint) {
		Counter.builder("security_rate_limit_triggered_total")
			.tag("endpoint", endpoint)
			.description("Rate Limit 트리거 횟수")
			.register(registry)
			.increment();
	}
}
