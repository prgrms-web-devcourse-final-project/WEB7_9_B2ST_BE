package com.back.b2st.domain.auth.service;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.auth.dto.LoginRequest;
import com.back.b2st.domain.auth.dto.TokenReissueRequest;
import com.back.b2st.domain.auth.entity.RefreshToken;
import com.back.b2st.domain.auth.repository.RefreshTokenRepository;
import com.back.b2st.global.jwt.JwtTokenProvider;
import com.back.b2st.global.jwt.dto.TokenInfo;
import com.back.b2st.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final AuthenticationManagerBuilder authenticationManagerBuilder;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenRepository refreshTokenRepository;

	@Transactional
	public TokenInfo login(LoginRequest request) {
		// Login ID/PW를 기반으로 Authentication 객체 생성
		// 아직 인증되지 않은 상태
		UsernamePasswordAuthenticationToken authenticationToken =
			new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());

		// 실제 검증 (사용자 비밀번호 체크)
		// authenticate() 메서드가 실행될 때 CustomUserDetailsService.loadUserByUsername 실행됨
		Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

		// 인증 정보를 기반으로 JWT 토큰 생성
		TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

		// RefreshToken Redis 저장 (expirationTime 설정을 위해 RedisHash 사용)
		refreshTokenRepository.save(new RefreshToken(authentication.getName(), tokenInfo.getRefreshToken()));

		return tokenInfo;
	}

	@Transactional
	public TokenInfo reissue(TokenReissueRequest request) {
		// Refresh Token 검증
		if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
			throw new IllegalArgumentException("Refresh Token이 유효하지 않습니다.");
		}

		// Access Token 서명 검증
		if (!jwtTokenProvider.validateTokenSignature(request.getAccessToken())) {
			throw new IllegalArgumentException("유효하지 않은 Access Token입니다.");
		}

		// Access Token 에서 Authentication 객체 가져오기
		Authentication authentication = jwtTokenProvider.getAuthentication(request.getAccessToken());

		// 이메일 추출
		String email;
		Object principal = authentication.getPrincipal();
		if (principal instanceof UserPrincipal userPrincipal) {
			email = userPrincipal.getEmail(); // UserPrincipal이면 여기서 이메일 추출
		} else {
			email = authentication.getName(); // 그 외의 경우(혹시 모를 호환성 있을까)
		}

		// Redis 에서 사용자의 Refresh Token 가져오기
		RefreshToken refreshToken = refreshTokenRepository.findById(email)
			.orElseThrow(() -> new IllegalArgumentException("로그아웃 된 사용자입니다."));

		// Redis 의 토큰과 요청 보낸 토큰 일치 여부 확인
		if (!refreshToken.getToken().equals(request.getRefreshToken())) {
			throw new IllegalArgumentException("토큰의 유저 정보가 일치하지 않습니다.");
		}

		// 새로운 토큰 생성
		TokenInfo newToken = jwtTokenProvider.generateToken(authentication);

		// Refresh Token Redis 업데이트
		refreshTokenRepository.save(new RefreshToken(email, newToken.getRefreshToken()));

		return newToken;
	}

	@Transactional
	public void logout(UserPrincipal principal) {
		refreshTokenRepository.deleteById(principal.getEmail());
	}
}
