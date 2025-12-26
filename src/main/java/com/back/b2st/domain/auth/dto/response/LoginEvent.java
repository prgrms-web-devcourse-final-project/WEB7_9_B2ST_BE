package com.back.b2st.domain.auth.dto.response;

import com.back.b2st.global.error.code.ErrorCode;

public record LoginEvent(
	String email,
	String clientIp,
	boolean isSuccess,
	String failReason,
	ErrorCode errorCode
) {

	// 성공 로그인 이벤트 생성 메서드
	public static LoginEvent success(String email, String clientIp) {
		return new LoginEvent(email, clientIp, true, null, null);
	}

	// 실패 로그인 이벤트 생성 메서드
	public static LoginEvent failure(String email, String clientIp, String message, ErrorCode errorCode) {
		return new LoginEvent(email, clientIp, false, message, errorCode);
	}
}
