package com.back.b2st.domain.auth.dto.oauth;

public record KakaoIdTokenPayload(
	// 발급자. ID 토큰을 발급한 인증 기관 정보
	String iss,
	// 대상. ID 토큰이 발급된 앱의 앱 키
	// client_id랑 일치해야 함
	String aud,
	// 카카오 회원번호. ID 토큰에 해당하는 사용자의 회원번호
	// = KakaoUserInfo.id
	String sub,
	// 토큰 발급 시각
	Long iat,
	// 만료 시간
	Long exp,
	// 인증 완료 시각
	Long auth_time,
	// 인가 코드 요청 시 전달한 nonce 값과 동일한 값
	// 리플레이 공격 방지용 (우리가 보낸 값과 일치해야 함)
	String nonce,

	//카카오 사용자 정보 클레임
	String nickname,
	String picture, // TODO: p1
	String email
) {
	public Long getKakaoId() {
		return sub != null ? Long.parseLong(sub) : null;
	}

	public boolean hasEmail() {
		return email != null && !email.isBlank();
	}
}
