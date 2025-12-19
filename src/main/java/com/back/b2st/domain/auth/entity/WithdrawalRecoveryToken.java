package com.back.b2st.domain.auth.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import lombok.Getter;

@Getter
@RedisHash("withdrawalRecovery")
public class WithdrawalRecoveryToken {

	@Id
	private String token; // UUID

	private String email;

	private Long memberId;

	private long ttl;

	private static final long DEFAULT_TTL = 3600 * 24; // 24시간

	public WithdrawalRecoveryToken(String token, String email, Long memberId) {
		this.token = token;
		this.email = email;
		this.memberId = memberId;
		this.ttl = DEFAULT_TTL;
	}
}
