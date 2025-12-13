package com.back.b2st.global.jwt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.back.b2st.domain.auth.Error.AuthErrorCode;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@InjectMocks
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private MockFilterChain filterChain;

	@BeforeEach
	void setUp() {
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		filterChain = new MockFilterChain();
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("유효한 토큰이 있는 경우, SecurityContext에 Authentication 객체 저장")
	void doFilter_withValidToken_shouldSetAuthenticationInContext() throws ServletException, IOException {
		// given
		String token = "valid-token";
		request.addHeader("Authorization", "Bearer " + token);

		Authentication authentication = new UsernamePasswordAuthenticationToken(
			"user@test.com", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
		);

		doNothing().when(jwtTokenProvider).validateToken(token);
		when(jwtTokenProvider.getAuthentication(token)).thenReturn(authentication);

		// when
		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		// then
		assertEquals(authentication, SecurityContextHolder.getContext().getAuthentication());
		assertNotNull(filterChain.getRequest());
	}

	@Test
	@DisplayName("만료된 토큰인 경우, request attribute에 EXPIRED_TOKEN 저장")
	void doFilter_withExpiredToken_shouldSetAttribute() throws ServletException, IOException {
		// given
		String token = "expired-token";
		request.addHeader("Authorization", "Bearer " + token);

		doThrow(new ExpiredJwtException(null, null, "Expired"))
			.when(jwtTokenProvider).validateToken(token);

		// when
		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		// then
		assertNull(SecurityContextHolder.getContext().getAuthentication()); // 인증 객체 없음
		// [중요] Attribute에 에러 코드가 잘 들어갔는지 확인
		assertEquals(AuthErrorCode.EXPIRED_TOKEN, request.getAttribute("exception"));
	}

	@Test
	@DisplayName("서명이 위조된 토큰인 경우, request attribute에 INVALID_ACCESS_TOKEN 저장")
	void doFilter_withInvalidSignatureToken_shouldSetAttribute() throws ServletException, IOException {
		// given
		String token = "invalid-sig-token";
		request.addHeader("Authorization", "Bearer " + token);

		doThrow(new SignatureException("Invalid signature"))
			.when(jwtTokenProvider).validateToken(token);

		// when
		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		// then
		assertNull(SecurityContextHolder.getContext().getAuthentication());
		assertEquals(AuthErrorCode.INVALID_ACCESS_TOKEN, request.getAttribute("exception"));
	}

	@Test
	@DisplayName("지원하지 않는 토큰인 경우, request attribute에 UNSUPPORTED_TOKEN 저장")
	void doFilter_withUnsupportedToken_shouldSetAttribute() throws ServletException, IOException {
		// given
		String token = "unsupported-token";
		request.addHeader("Authorization", "Bearer " + token);

		doThrow(new UnsupportedJwtException("Unsupported"))
			.when(jwtTokenProvider).validateToken(token);

		// when
		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		// then
		assertNull(SecurityContextHolder.getContext().getAuthentication());
		assertEquals(AuthErrorCode.UNSUPPORTED_TOKEN, request.getAttribute("exception"));
	}

	@Test
	@DisplayName("토큰이 없는 경우, 아무 동작 없이 다음 필터로 진행")
	void doFilter_withoutToken_shouldDoNothing() throws ServletException, IOException {
		// given
		// No Authorization header

		// when
		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		// then
		assertNull(SecurityContextHolder.getContext().getAuthentication());
		assertNull(request.getAttribute("exception")); // 에러 속성도 없어야 함
		assertNotNull(filterChain.getRequest());
	}

	@Test
	@DisplayName("토큰 형식이 'Bearer '가 아닌 경우, 아무 동작 없이 다음 필터로 진행")
	void doFilter_withInvalidBearerFormat_shouldDoNothing() throws ServletException, IOException {
		// given
		String token = "invalid-format-token";
		request.addHeader("Authorization", token); // Prefix missing

		// when
		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		// then
		assertNull(SecurityContextHolder.getContext().getAuthentication());
		assertNull(request.getAttribute("exception"));
		assertNotNull(filterChain.getRequest());
	}
}
