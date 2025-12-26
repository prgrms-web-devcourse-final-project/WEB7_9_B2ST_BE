package com.back.b2st.domain.auth.client;

import com.back.b2st.domain.auth.dto.oauth.KakaoIdTokenPayload;

public interface KakaoApiClient {
	// // 인가 코드로 카카오 액세스 토큰 요청
	// KakaoTokenRes getToken(String code);
	//
	// // 액세스 토큰으로 사용자 정보 조회
	// KakaoUserInfo getUserInfo(String accessToken);

	// OIDC
	KakaoIdTokenPayload getTokenAndParseIdToken(String code);
}
