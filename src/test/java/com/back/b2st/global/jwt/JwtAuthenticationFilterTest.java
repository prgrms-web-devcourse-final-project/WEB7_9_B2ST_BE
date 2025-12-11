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

		when(jwtTokenProvider.validateToken(token)).thenReturn(true);
		when(jwtTokenProvider.getAuthentication(token)).thenReturn(authentication);

		// when
		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		// then
		assertEquals(authentication, SecurityContextHolder.getContext().getAuthentication());
		assertNotNull(filterChain.getRequest()); // 필터 체인이 계속 진행되었는지 확인
	}

	@Test
	@DisplayName("유효하지 않은 토큰이 있는 경우, SecurityContext는 비어있음")
	void doFilter_withInvalidToken_shouldNotSetAuthenticationInContext() throws ServletException, IOException {
		// given
		String token = "invalid-token";
		request.addHeader("Authorization", "Bearer " + token);

		when(jwtTokenProvider.validateToken(token)).thenReturn(false);

		// when
		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		// then
		assertNull(SecurityContextHolder.getContext().getAuthentication());
		assertNotNull(filterChain.getRequest());
	}

	@Test
	@DisplayName("토큰이 없는 경우, SecurityContext는 비어있")
	void doFilter_withoutToken_shouldNotSetAuthenticationInContext() throws ServletException, IOException {
		// given
		// No Authorization header

		// when
		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		// then
		assertNull(SecurityContextHolder.getContext().getAuthentication());
		assertNotNull(filterChain.getRequest());
	}

	@Test
	@DisplayName("토큰이 'Bearer '로 시작하지 않는 경우, SecurityContext는 비어있음")
	void doFilter_withInvalidBearerFormat_shouldNotSetAuthenticationInContext() throws ServletException, IOException {
		// given
		String token = "invalid-bearer-token";
		request.addHeader("Authorization", token); // "Bearer " prefix is missing

		// when
		jwtAuthenticationFilter.doFilter(request, response, filterChain);

		// then
		assertNull(SecurityContextHolder.getContext().getAuthentication());
		assertNotNull(filterChain.getRequest());
	}
}
