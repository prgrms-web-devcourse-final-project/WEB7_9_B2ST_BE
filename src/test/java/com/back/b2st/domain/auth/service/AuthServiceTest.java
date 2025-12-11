package com.back.b2st.domain.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import com.back.b2st.domain.auth.dto.LoginRequest;
import com.back.b2st.domain.auth.dto.TokenReissueRequest;
import com.back.b2st.domain.auth.entity.RefreshToken;
import com.back.b2st.domain.auth.repository.RefreshTokenRepository;
import com.back.b2st.global.jwt.JwtTokenProvider;
import com.back.b2st.global.jwt.dto.TokenInfo;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@InjectMocks
	private AuthService authService;

	@Mock
	private AuthenticationManagerBuilder authenticationManagerBuilder;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Test
	@DisplayName("로그인 성공 시 토큰 발급 및 Redis 저장 검증")
	void login_success() {
		// given
		LoginRequest request = new LoginRequest();
		ReflectionTestUtils.setField(request, "email", "test@test.com");
		ReflectionTestUtils.setField(request, "password", "Password123!");

		// AuthenticationManager Mocking (Builder -> Manager -> Authentication)
		AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
		Authentication authentication = mock(Authentication.class);

		given(authenticationManagerBuilder.getObject()).willReturn(authenticationManager);
		given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.willReturn(authentication);
		given(authentication.getName()).willReturn("test@test.com");

		// TokenProvider Mocking
		TokenInfo expectedToken = TokenInfo.builder()
			.grantType("Bearer")
			.accessToken("access")
			.refreshToken("refresh")
			.build();
		given(jwtTokenProvider.generateToken(authentication)).willReturn(expectedToken);

		// when
		TokenInfo result = authService.login(request);

		// then
		assertThat(result.getAccessToken()).isEqualTo("access");
		assertThat(result.getRefreshToken()).isEqualTo("refresh");

		// Redis Repository의 save 메서드가 호출되었는지 확인
		verify(refreshTokenRepository).save(any(RefreshToken.class));
	}

	@Test
	@DisplayName("토큰 재발급 성공")
	void reissue_success() {
		// given
		TokenReissueRequest request = new TokenReissueRequest();
		ReflectionTestUtils.setField(request, "accessToken", "oldAccess");
		ReflectionTestUtils.setField(request, "refreshToken", "validRefresh");

		// Mocking
		Authentication authentication = mock(Authentication.class);
		given(authentication.getName()).willReturn("test@test.com");

		// 1. Refresh Token 유효성 검사 통과 가정
		given(jwtTokenProvider.validateToken("validRefresh")).willReturn(true);
		given(jwtTokenProvider.validateTokenSignature("oldAccess")).willReturn(true);
		// 2. Access Token에서 인증 정보 추출
		given(jwtTokenProvider.getAuthentication("oldAccess")).willReturn(authentication);
		// 3. Redis에 저장된 토큰 조회
		RefreshToken storedToken = new RefreshToken("test@test.com", "validRefresh");
		given(refreshTokenRepository.findById("test@test.com")).willReturn(java.util.Optional.of(storedToken));

		// 4. 새 토큰 생성
		TokenInfo newToken = TokenInfo.builder()
			.accessToken("newAccess")
			.refreshToken("newRefresh")
			.build();
		given(jwtTokenProvider.generateToken(authentication)).willReturn(newToken);

		// when
		TokenInfo result = authService.reissue(request);

		// then
		assertThat(result.getAccessToken()).isEqualTo("newAccess");
		assertThat(result.getRefreshToken()).isEqualTo("newRefresh");
		verify(refreshTokenRepository).save(any(RefreshToken.class)); // Redis 업데이트 확인
	}

	@Test
	@DisplayName("토큰 재발급 실패 - 저장된 토큰 불일치")
	void reissue_fail_token_mismatch() {
		// given
		TokenReissueRequest request = new TokenReissueRequest();
		ReflectionTestUtils.setField(request, "accessToken", "oldAccess");
		ReflectionTestUtils.setField(request, "refreshToken", "hackRefresh");

		Authentication authentication = mock(Authentication.class);
		given(authentication.getName()).willReturn("test@test.com");

		given(jwtTokenProvider.validateToken("hackRefresh")).willReturn(true);
		given(jwtTokenProvider.validateTokenSignature("oldAccess")).willReturn(true);
		given(jwtTokenProvider.getAuthentication("oldAccess")).willReturn(authentication);

		// Redis에는 "originRefresh"가 저장되어 있음
		RefreshToken storedToken = new RefreshToken("test@test.com", "originRefresh");
		given(refreshTokenRepository.findById("test@test.com")).willReturn(java.util.Optional.of(storedToken));

		// when & then
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> authService.reissue(request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("토큰의 유저 정보가 일치하지 않습니다.");
	}
}
