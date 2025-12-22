package com.back.b2st.domain.auth.dto.response;

public record KakaoAuthorizeUrlRes(
	String authorizeUrl,
	String state, // CSRF 방지용
	String nonce // 리플레이 방지용
) {
}
