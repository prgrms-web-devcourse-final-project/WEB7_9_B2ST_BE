package com.back.b2st.global.jwt;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import com.back.b2st.domain.auth.Error.AuthErrorCode;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends GenericFilterBean {

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
		ServletException {
		// Request Header 에서 JWT 토큰 추출
		String token = resolveToken((HttpServletRequest)request);

		try {
			// 토큰 존재, 유효성 검사
			if (token != null) {
				jwtTokenProvider.validateToken(token);
				Authentication authentication = jwtTokenProvider.getAuthentication(token);
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		} catch (ExpiredJwtException e) {
			// 토큰 만료 -> EXPIRED_TOKEN
			log.debug("Expired JWT token: {}", e.getMessage());
			request.setAttribute("exception", AuthErrorCode.EXPIRED_TOKEN);
		} catch (io.jsonwebtoken.security.SignatureException e) {
			// 위조/손상 -> INVALID_ACCESS_TOKEN
			log.debug("Invalid JWT signature: {}", e.getMessage());
			request.setAttribute("exception", AuthErrorCode.INVALID_ACCESS_TOKEN);
		} catch (UnsupportedJwtException e) {
			// 지원하지 않는 형식 -> UNSUPPORTED_TOKEN
			log.debug("Unsupported JWT token: {}", e.getMessage());
			request.setAttribute("exception", AuthErrorCode.UNSUPPORTED_TOKEN);
		} catch (Exception e) {
			// 그 외 오류 -> INVALID_ACCESS_TOKEN
			log.debug("JWT processing error: {}", e.getMessage());
			request.setAttribute("exception", AuthErrorCode.INVALID_ACCESS_TOKEN);
		}

		// 인증 실패 시 AuthenticationEntryPoint 처리
		chain.doFilter(request, response);
	}

	// Request Header 에서 토큰 정보 추출
	private String resolveToken(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}
}
