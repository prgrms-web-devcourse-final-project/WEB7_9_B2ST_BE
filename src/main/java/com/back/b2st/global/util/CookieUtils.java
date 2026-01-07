package com.back.b2st.global.util;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CookieUtils {

	private static final String REFRESH_TOKEN_NAME = "refreshToken";
	private static final Duration REFRESH_TOKEN_MAX_AGE = Duration.ofDays(7);

	public static void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
		ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_NAME, refreshToken)
			.httpOnly(true)
			.secure(true)
			.path("/")
			.maxAge(REFRESH_TOKEN_MAX_AGE)
			.sameSite("None")
			.build();

		response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	// 로그아웃 시 리프레쉬 토큰 쿠키 삭제
	public static void clearRefreshTokenCookie(HttpServletResponse response) {
		ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_NAME, "")
			.maxAge(0) // 즉시 만료
			.path("/")
			.build();

		response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}
}
