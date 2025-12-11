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

		// Principal 타입에 따라 ID 추출 로직 분기
		Object principal = authentication.getPrincipal();
		Long memberId;

		if (principal instanceof CustomUserDetails userDetails) {
			// 초기 로그인 시점
			memberId = userDetails.getId();
		} else if (principal instanceof UserPrincipal userPrincipal) {
			// 토큰 재발급 시점 (토큰에서 복원된 객체)
			memberId = userPrincipal.getId();
		} else {
			// 예외 처리 (혹시 모를 상황)
			throw new IllegalArgumentException("지원하지 않는 인증 객체입니다.");
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
			throw new RuntimeException("권한 정보가 없는 토큰입니다.");
		}

		Collection<? extends GrantedAuthority> authorities =
			Arrays.stream(claims.get("auth").toString().split(","))
				.map(SimpleGrantedAuthority::new)
				.collect(Collectors.toList());

		// Claims에서 ID 꺼내서 UserPrincipal 생성
		// DB 조회 없이 토큰 정보로만
		Long userId = claims.get("id", Long.class);

		UserPrincipal principal = UserPrincipal.builder()
			.id(userId)
			.email(claims.getSubject())
			.role(authorities.iterator().next().getAuthority())
			.build();

		return new UsernamePasswordAuthenticationToken(principal, "", authorities);
	}

	// 토큰 유효성 검증
	public boolean validateToken(String token) {
		try {
			Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
			return true;
		} catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
			log.info("잘못된 JWT 서명입니다.");
		} catch (ExpiredJwtException e) {
			log.info("만료된 JWT 토큰입니다.");
		} catch (UnsupportedJwtException e) {
			log.info("지원되지 않는 JWT 토큰입니다.");
		} catch (IllegalArgumentException e) {
			log.info("JWT 토큰이 잘못되었습니다.");
		}
		return false;
	}

	private Claims parseClaims(String accessToken) {
		try {
			return Jwts.parser().verifyWith(key).build().parseSignedClaims(accessToken).getPayload();
		} catch (ExpiredJwtException e) {
			return e.getClaims();
		}
	}
}
