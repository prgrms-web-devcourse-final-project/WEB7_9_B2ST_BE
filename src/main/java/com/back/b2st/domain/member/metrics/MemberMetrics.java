package com.back.b2st.domain.member.metrics;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class MemberMetrics {

	private final Counter signupCounter;
	private final Counter withdrawCounter;
	private final Counter passwordChangeCounter;

	public MemberMetrics(MeterRegistry registry) {
		this.signupCounter = Counter.builder("member_signup_total")
			.description("회원가입 횟수")
			.register(registry);

		this.withdrawCounter = Counter.builder("member_withdraw_total")
			.description("회원탈퇴 횟수")
			.register(registry);

		this.passwordChangeCounter = Counter.builder("member_password_change_total")
			.description("비밀번호 변경 횟수")
			.register(registry);
	}

	public void recordSignup() {
		signupCounter.increment();
	}

	public void recordWithdraw() {
		withdrawCounter.increment();
	}

	public void recordPasswordChange() {
		passwordChangeCounter.increment();
	}
}
