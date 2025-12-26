package com.back.b2st.domain.auth.service;

import com.back.b2st.domain.auth.dto.request.LoginReq;
import com.back.b2st.domain.auth.dto.request.TokenReissueReq;

public class AuthTestRequestBuilder {
	// 이 밑으로 생성자
	public static LoginReq buildLoginRequest(String email, String password) {
		return new LoginReq(email, password);
	}

	public static TokenReissueReq buildTokenReissueRequest(String accessToken, String refreshToken) {
		return new TokenReissueReq(accessToken, refreshToken);
	}
}
