package com.back.b2st.global.jwt.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record TokenInfo(
	String grantType, // Bearer
	String accessToken,
	@JsonIgnore
	String refreshToken
) {
}
