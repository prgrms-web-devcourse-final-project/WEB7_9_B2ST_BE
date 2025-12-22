package com.back.b2st.domain.auth.dto.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoTokenRes(
	@JsonProperty("token_type")
	String tokenType,

	@JsonProperty("access_token")
	String accessToken,

	@JsonProperty("expires_in")
	Integer expiresIn,

	@JsonProperty("refresh_token")
	String refreshToken,

	@JsonProperty("refresh_token_expires_in")
	Integer refreshTokenExpiresIn,

	@JsonProperty("scope")
	String scope,

	// OIDC id token
	@JsonProperty("id_token")
	String idToken
) {
}
