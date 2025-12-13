package com.back.b2st.domain.auth.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@RedisHash(value = "refreshToken", timeToLive = 604800) // 7일 (TTL)
public class RefreshToken {

	@Id
	private String email; // Key: 이메일

	private String token; // Value: Refresh Token 값
}
