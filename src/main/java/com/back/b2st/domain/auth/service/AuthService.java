package com.back.b2st.domain.auth.service;

import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.auth.dto.request.LoginReq;
import com.back.b2st.domain.auth.entity.RefreshToken;
import com.back.b2st.domain.auth.error.AuthErrorCode;
import com.back.b2st.domain.auth.repository.RefreshTokenRepository;
import com.back.b2st.global.error.exception.BusinessException;
import com.back.b2st.global.jwt.JwtTokenProvider;
import com.back.b2st.global.jwt.dto.response.TokenInfo;
import com.back.b2st.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

	private final AuthenticationManagerBuilder authenticationManagerBuilder;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenRepository refreshTokenRepository;

	@Transactional
	public TokenInfo login(LoginReq request) {
		// Login ID/PW를 기반으로 Authentication 객체 생성
		UsernamePasswordAuthenticationToken authenticationToken =
			new UsernamePasswordAuthenticationToken(request.email(), request.password());

		// 실제 검증 (사용자 비밀번호 체크)
		// authenticate() 실행 시 CustomUserDetailsService.loadUserByUsername 호출됨
		// 실패 시 BadCredentialsException 발생 -> GlobalExceptionHandler가 처리
		Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

		// 인증 정보를 기반으로 JWT 토큰 생성
		TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

		// RefreshToken Redis 저장
		String family = UUID.randomUUID().toString();

		refreshTokenRepository.save(new RefreshToken(
			authentication.getName(),
			tokenInfo.refreshToken(),
			family,
			1L
		));

		return tokenInfo;
	}

	@Transactional
	public TokenInfo reissue(String accessToken, String refreshToken) {
		// Refresh Token 검증
		validateToken(refreshToken);

		// Access Token 서명 검증 (만료 여부는 무시하고 서명만 확인)
		validateTokenSignature(accessToken);

		// Access Token에서 Authentication 객체 추출 (만료된 토큰이어도 파싱 가능)
		Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);

		// 이메일 추출 (UserPrincipal 타입 체크)
		String email;
		Object principal = authentication.getPrincipal();
		if (principal instanceof UserPrincipal userPrincipal) {
			email = userPrincipal.getEmail();
		} else {
			email = authentication.getName();
		}

		// Redis에서 사용자의 Refresh Token 조회
		RefreshToken storedToken = refreshTokenRepository.findById(email)
			.orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_TOKEN));

		// Redis의 토큰과 요청받은 토큰 일치 여부 확인
		if (!storedToken.getToken().equals(refreshToken)) {
			refreshTokenRepository.deleteById(email);
			log.warn("🚨 토큰 탈취 감지! (Token Reuse Detected) User: {}", email);
			throw new BusinessException(AuthErrorCode.TOKEN_REUSE_DETECTED);
		}

		// 새로운 토큰 생성
		TokenInfo newToken = jwtTokenProvider.generateToken(authentication);

		// Refresh Token Redis 업데이트
		refreshTokenRepository.save(new RefreshToken(
			email,
			newToken.refreshToken(),
			storedToken.getFamily(),
			storedToken.getGeneration() + 1
		));

		return newToken;
	}

	@Transactional
	public void logout(UserPrincipal principal) {
		refreshTokenRepository.deleteById(principal.getEmail());
	}

	// 이 밑으로 validate
	private void validateToken(String refreshToken) {
		// validateToken은 실패 시 예외를 던짐. 잡아서 커스텀 에러로 변환
		try {
			jwtTokenProvider.validateToken(refreshToken);
		} catch (Exception e) {
			throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
		}
	}

	private void validateTokenSignature(String accessToken) {
		if (!jwtTokenProvider.validateTokenSignature(accessToken)) {
			throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
		}
	}
}
