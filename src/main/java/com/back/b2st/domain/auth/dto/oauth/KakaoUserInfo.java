package com.back.b2st.domain.auth.dto.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserInfo(
	Long id,
	@JsonProperty("kakao_account")
	KakaoAccount kakaoAccount
) {
	public record KakaoAccount(
		String email,
		@JsonProperty("is_email_valid")
		Boolean isEmailValid,
		@JsonProperty("is_email_verified")
		Boolean isEmailVerified,
		KakaoProfile profile
	) {
	}

	public record KakaoProfile(
		String nickname,
		@JsonProperty("profile_image_url")
		String profileImageUrl // TODO: p1를 위해 남겨놓음
	) {
	}

	public String getEmail() {
		return kakaoAccount != null ? kakaoAccount.email() : null;
	}

	public String getNickname() {
		if (kakaoAccount != null && kakaoAccount.profile() != null) {
			return kakaoAccount.profile().nickname();
		}
		return null;
	}

	public boolean isEmailVerified() {
		return kakaoAccount != null
			&& Boolean.TRUE.equals(kakaoAccount.isEmailVerified());
	}
}
