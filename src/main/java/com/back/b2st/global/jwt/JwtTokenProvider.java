package com.back.b2st.global.jwt;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.back.b2st.domain.auth.Error.AuthErrorCode;
import com.back.b2st.global.error.exception.BusinessException;
import com.back.b2st.global.jwt.dto.TokenInfo;
import com.back.b2st.security.CustomUserDetails;
import com.back.b2st.security.UserPrincipal;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtTokenProvider {

	private final SecretKey key;
	private final long accessTokenValidity;
	private final long refreshTokenValidity;

	public JwtTokenProvider(@Value("${jwt.secret}") String secret,
		@Value("${jwt.access-expiration}") long accessTokenValidity,
		@Value("${jwt.refresh-expiration}") long refreshTokenValidity) {
		byte[] keyBytes = Decoders.BASE64.decode(secret);
		this.key = Keys.hmacShaKeyFor(keyBytes);
		this.accessTokenValidity = accessTokenValidity;
		this.refreshTokenValidity = refreshTokenValidity;
	}

	public TokenInfo generateToken(Authentication authentication) {
		String authorities = authentication.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.collect(Collectors.joining(","));

		long now = (new Date()).getTime();

		Object principal = authentication.getPrincipal();
		Long memberId;

		if (principal instanceof CustomUserDetails userDetails) {
			memberId = userDetails.getId();
		} else if (principal instanceof UserPrincipal userPrincipal) {
			memberId = userPrincipal.getId();
		} else {
			throw new BusinessException(AuthErrorCode.UNSUPPORTED_TOKEN);
		}

		// Access Token 빌더
		String accessToken = Jwts.builder()
			.subject(authentication.getName())
			.claim("auth", authorities)
			.claim("id", memberId)
			.expiration(new Date(now + accessTokenValidity))
			.signWith(key)
			.compact();

		// Refresh Token 빌더
		String refreshToken = Jwts.builder()
			.expiration(new Date(now + refreshTokenValidity))
			.signWith(key)
			.compact();

		return TokenInfo.builder()
			.grantType("Bearer")
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.build();
	}

	// 토큰에서 인증 정보 조회 로직
	public Authentication getAuthentication(String accessToken) {
		Claims claims = parseClaims(accessToken);

		if (claims.get("auth") == null) {
			throw new BusinessException(AuthErrorCode.EMPTY_CLAIMS);
		}

		Collection<? extends GrantedAuthority> authorities =
			Arrays.stream(claims.get("auth").toString().split(","))
				.map(SimpleGrantedAuthority::new)
				.collect(Collectors.toList());

		Long userId = claims.get("id", Long.class);

		UserPrincipal principal = UserPrincipal.builder()
			.id(userId)
			.email(claims.getSubject())
			.role(authorities.iterator().next().getAuthority())
			.build();

		return new UsernamePasswordAuthenticationToken(principal, "", authorities);
	}

	// 토큰 유효성 검증
	public void validateToken(String token) {
		try {
			Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
		} catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
			throw new io.jsonwebtoken.security.SignatureException("잘못된 JWT 서명입니다.");
		} catch (ExpiredJwtException e) {
			throw new ExpiredJwtException(null, null, "만료된 JWT 토큰입니다.");
		} catch (UnsupportedJwtException e) {
			throw new UnsupportedJwtException("지원되지 않는 JWT 토큰입니다.");
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("JWT 토큰이 잘못되었습니다.");
		}
	}

	// 토큰 서명 검증
	public boolean validateTokenSignature(String token) {
		try {
			Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
			return true;
		} catch (ExpiredJwtException e) {
			return true;
		} catch (Exception e) {
			log.info("JWT 서명 검증 실패: {}", e.getMessage());
			return false;
		}
	}

	private Claims parseClaims(String accessToken) {
		try {
			return Jwts.parser().verifyWith(key).build().parseSignedClaims(accessToken).getPayload();
		} catch (ExpiredJwtException e) {
			return e.getClaims();
		}
	}
}
