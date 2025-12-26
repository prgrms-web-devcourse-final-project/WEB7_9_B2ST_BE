package com.back.b2st.global.util;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import jakarta.servlet.http.HttpServletResponse;

@DisplayName("CookieUtils 테스트")
class CookieUtilsTest {

	private HttpServletResponse response;

	@BeforeEach
	void setUp() {
		response = mock(HttpServletResponse.class);
	}

	@Nested
	@DisplayName("setRefreshTokenCookie")
	class SetRefreshTokenCookieTest {

		@Test
		@DisplayName("성공 - 올바른 쿠키 설정")
		void success() {
			// given
			String refreshToken = "test-refresh-token";

			// when
			CookieUtils.setRefreshTokenCookie(response, refreshToken);

			// then
			ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
			verify(response).setHeader(eq("Set-Cookie"), headerCaptor.capture());

			String cookieValue = headerCaptor.getValue();
			assertThat(cookieValue).contains("refreshToken=test-refresh-token");
			assertThat(cookieValue).contains("Path=/");
			assertThat(cookieValue).contains("HttpOnly");
			assertThat(cookieValue).contains("Secure");
			assertThat(cookieValue).contains("SameSite=None");
			assertThat(cookieValue).contains("Max-Age=604800"); // 7일 = 604800초
		}
	}

	@Nested
	@DisplayName("clearRefreshTokenCookie")
	class ClearRefreshTokenCookieTest {

		@Test
		@DisplayName("성공 - 쿠키 삭제 (Max-Age=0)")
		void success() {
			// when
			CookieUtils.clearRefreshTokenCookie(response);

			// then
			ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
			verify(response).setHeader(eq("Set-Cookie"), headerCaptor.capture());

			String cookieValue = headerCaptor.getValue();
			assertThat(cookieValue).contains("refreshToken=");
			assertThat(cookieValue).contains("Max-Age=0"); // 즉시 만료
			assertThat(cookieValue).contains("Path=/");
		}
	}
}
