package com.back.b2st.domain.auth.metrics;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class AuthMetrics {

	private final MeterRegistry registry;
	private final AtomicInteger lockedAccountCount = new AtomicInteger(0);

	// Counters
	private final Counter loginSuccessCounter;
	private final Counter loginFailureEmailCounter;
	private final Counter loginFailureKakaoCounter;
	private final Counter accountLockCounter;
	private final Counter tokenReissueCounter;
	private final Counter logoutCounter;

	public AuthMetrics(MeterRegistry registry) {
		this.registry = registry;

		// 로그인 성공 카운터
		this.loginSuccessCounter = Counter.builder("auth_login_total")
			.tag("result", "success")
			.tag("provider", "EMAIL")
			.description("로그인 성공 횟수")
			.register(registry);

		// 이메일 로그인 실패 카운터
		this.loginFailureEmailCounter = Counter.builder("auth_login_total")
			.tag("result", "failure")
			.tag("provider", "EMAIL")
			.description("이메일 로그인 실패 횟수")
			.register(registry);

		// 카카오 로그인 실패 카운터
		this.loginFailureKakaoCounter = Counter.builder("auth_login_total")
			.tag("result", "failure")
			.tag("provider", "KAKAO")
			.description("카카오 로그인 실패 횟수")
			.register(registry);

		// 계정 잠금 카운터
		this.accountLockCounter = Counter.builder("auth_account_locked_total")
			.description("계정 잠금 발생 횟수")
			.register(registry);

		// 토큰 재발급 카운터
		this.tokenReissueCounter = Counter.builder("auth_token_reissue_total")
			.description("토큰 재발급 횟수")
			.register(registry);

		// 로그아웃 카운터
		this.logoutCounter = Counter.builder("auth_logout_total").description("로그아웃 횟수").register(registry);

		// 현재 잠긴 계정 수 (Gauge)
		Gauge.builder("auth_locked_account_count", lockedAccountCount, AtomicInteger::get)
			.description("현재 잠긴 계정 수")
			.register(registry);
	}

	public void recordLoginSuccess(String provider) {
		loginSuccessCounter.increment();
	}

	public void recordLoginFailure(String provider, String reason) {
		if ("KAKAO".equals(provider)) {
			loginFailureKakaoCounter.increment();
		} else {
			loginFailureEmailCounter.increment();
		}

		// 실패 사유별 카운터 (동적 생성)
		Counter.builder("auth_login_failure_reason_total")
			.tag("reason", reason)
			.register(registry)
			.increment();
	}

	public void recordAccountLock() {
		accountLockCounter.increment();
		lockedAccountCount.incrementAndGet();
	}

	public void recordAccountUnlock() {
		lockedAccountCount.decrementAndGet();
	}

	public void recordTokenReissue() {
		tokenReissueCounter.increment();
	}

	public void recordLogout() {
		logoutCounter.increment();
	}

	public void setLockedAccountCount(int count) {
		lockedAccountCount.set(count);
	}
}
