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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.back.b2st.global.jwt.dto.TokenInfo;

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

	// 유저 정보를 가지고 AccessToken, RefreshToken 생성
	public TokenInfo generateToken(Authentication authentication) {
		// 권한 가져오기
		String authorities = authentication.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.collect(Collectors.joining(","));

		long now = (new Date()).getTime();

		// Access Token 생성
		String accessToken = Jwts.builder().subject(authentication.getName()) // 이메일
			.claim("auth", authorities)        // 권한 정보 저장
			.expiration(new Date(now + accessTokenValidity)).signWith(key).compact();

		// Refresh Token 생성
		String refreshToken = Jwts.builder().expiration(new Date(now + refreshTokenValidity)).signWith(key).compact();

		return TokenInfo.builder().grantType("Bearer").accessToken(accessToken).refreshToken(refreshToken).build();
	}

	// JWT 토큰을 복호화하여 토큰에 들어있는 정보 꺼내기
	public Authentication getAuthentication(String accessToken) {
		// 토큰 복호화
		Claims claims = parseClaims(accessToken);

		if (claims.get("auth") == null) {
			throw new RuntimeException("권한 정보가 없는 토큰입니다.");
		}

		// 클레임에서 권한 정보 가져오기
		Collection<? extends GrantedAuthority> authorities = Arrays.stream(claims.get("auth").toString().split(","))
			.map(SimpleGrantedAuthority::new)
			.collect(Collectors.toList());

		// UserDetails 객체를 만들어서 Authentication 리턴
		UserDetails principal = new User(claims.getSubject(), "", authorities);
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

	// 토큰 서명 검증
	public boolean validateTokenSignature(String token) {
		try {
			Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
			return true;
		} catch (ExpiredJwtException e) {
			// 만료된 토큰이라도 서명이 정상이면 true
			return true;
		} catch (Exception e) {
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
