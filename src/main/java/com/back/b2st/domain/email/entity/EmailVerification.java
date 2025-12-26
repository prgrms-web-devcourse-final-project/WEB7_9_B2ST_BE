package com.back.b2st.domain.email.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "emailVerification", timeToLive = 300)
public class EmailVerification {

	@Id
	private String email;
	private String code;
	private int attemptCount; // 인증 시도 횟수

	// 인증 시도 횟수 증가
	public EmailVerification incrementAttempt() {
		return EmailVerification.builder()
			.email(this.email)
			.code(this.code)
			.attemptCount(this.attemptCount + 1)
			.build();
	}

	public boolean isMaxAttemptExceeded() {
		return this.attemptCount >= 5;
	}

}
