package com.back.b2st.global.jwt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class TokenInfo {
	private String grantType;   // Bearer
	private String accessToken;
	private String refreshToken;
}
