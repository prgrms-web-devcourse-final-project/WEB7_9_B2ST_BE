package com.back.b2st.global.jwt.dto.response;

public record TokenInfo(
	String grantType, // Bearer
	String accessToken,
	String refreshToken
) {
}
