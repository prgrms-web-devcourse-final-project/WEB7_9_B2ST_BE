package com.back.b2st.domain.auth.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 프론트에서 생성한 nonce 저장, 카카오 콜백 시 검증에 사용
@Getter
@AllArgsConstructor
@RedisHash("oauth_nonce")
public class OAuthNonce {

	@Id
	private String nonce; // 랜덤 UUID
	private String state;
	@TimeToLive
	private Long ttl; // 5분

	public static OAuthNonce create(String nonce, String state) {
		return new OAuthNonce(nonce, state, 300L);
	}
}
