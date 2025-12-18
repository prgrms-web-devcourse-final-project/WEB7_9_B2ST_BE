package com.back.b2st.domain.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;
import java.util.UUID;

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

import com.back.b2st.domain.auth.dto.request.LoginReq;
import com.back.b2st.domain.auth.entity.RefreshToken;
import com.back.b2st.domain.auth.error.AuthErrorCode;
import com.back.b2st.domain.auth.repository.RefreshTokenRepository;
import com.back.b2st.global.error.exception.BusinessException;
import com.back.b2st.global.jwt.JwtTokenProvider;
import com.back.b2st.global.jwt.dto.response.TokenInfo;
import com.back.b2st.security.UserPrincipal;

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
		LoginReq request = new LoginReq("test@test.com", "Password123!");

		AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
		Authentication authentication = mock(Authentication.class);

		given(authenticationManagerBuilder.getObject()).willReturn(authenticationManager);
		given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.willReturn(authentication);
		given(authentication.getName()).willReturn("test@test.com");

		TokenInfo expectedToken = new TokenInfo("Bearer", "access", "refresh");
		given(jwtTokenProvider.generateToken(authentication)).willReturn(expectedToken);

		// when
		TokenInfo result = authService.login(request);

		// then
		assertThat(result.accessToken()).isEqualTo("access");
		assertThat(result.refreshToken()).isEqualTo("refresh");
		verify(refreshTokenRepository).save(any(RefreshToken.class));
	}

	@Test
	@DisplayName("토큰 재발급 성공")
	void reissue_success() {
		// given
		String oldAccessToken = "oldAccess";
		String validRefreshToken = "validRefresh";
		String email = "test@test.com";
		String familyId = UUID.randomUUID().toString();

		Authentication authentication = mock(Authentication.class);
		UserPrincipal principal = UserPrincipal.builder().email(email).id(1L).build();
		given(authentication.getPrincipal()).willReturn(principal);

		willDoNothing().given(jwtTokenProvider).validateToken(validRefreshToken);

		given(jwtTokenProvider.validateTokenSignature(oldAccessToken)).willReturn(true);
		given(jwtTokenProvider.getAuthentication(oldAccessToken)).willReturn(authentication);

		// Redis 조회 (기존 토큰: generation 1)
		RefreshToken storedToken = new RefreshToken(email, validRefreshToken, familyId, 1L);
		given(refreshTokenRepository.findById(email)).willReturn(Optional.of(storedToken));

		TokenInfo newToken = new TokenInfo("Bearer", "newAccess", "newRefresh");
		given(jwtTokenProvider.generateToken(authentication)).willReturn(newToken);

		// when
		TokenInfo result = authService.reissue(oldAccessToken, validRefreshToken);

		// then
		assertThat(result.accessToken()).isEqualTo("newAccess");
		assertThat(result.refreshToken()).isEqualTo("newRefresh");
		verify(refreshTokenRepository).save(any(RefreshToken.class));
	}

	@Test
	@DisplayName("토큰 재발급 실패 - 저장된 토큰 불일치")
	void reissue_fail_token_mismatch() {
		// given
		String oldAccessToken = "oldAccess";
		String hackRefreshToken = "hackRefresh";
		String email = "test@test.com";

		Authentication authentication = mock(Authentication.class);
		UserPrincipal principal = UserPrincipal.builder().email(email).id(1L).build();
		given(authentication.getPrincipal()).willReturn(principal);

		willDoNothing().given(jwtTokenProvider).validateToken(hackRefreshToken);
		given(jwtTokenProvider.validateTokenSignature(oldAccessToken)).willReturn(true);
		given(jwtTokenProvider.getAuthentication(oldAccessToken)).willReturn(authentication);

		RefreshToken storedToken = new RefreshToken(email, "originRefresh", UUID.randomUUID().toString(), 1L);
		given(refreshTokenRepository.findById(email)).willReturn(Optional.of(storedToken));

		// when & then
		assertThatThrownBy(() -> authService.reissue(oldAccessToken, hackRefreshToken))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(AuthErrorCode.TOKEN_REUSE_DETECTED);

		verify(refreshTokenRepository).deleteById(email);
	}

	@Test
	@DisplayName("토큰 재발급 실패 - 유효하지 않은 Refresh Token (예외 발생)")
	void reissue_fail_invalid_refresh_token() {
		// given
		String invalidRefreshToken = "invalidRefresh";
		String oldAccessToken = "oldAccess";

		willThrow(new io.jsonwebtoken.security.SignatureException("Invalid signature"))
			.given(jwtTokenProvider).validateToken(invalidRefreshToken);

		// when & then
		assertThatThrownBy(() -> authService.reissue(oldAccessToken, invalidRefreshToken))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(AuthErrorCode.INVALID_TOKEN);
	}

	@Test
	@DisplayName("로그아웃 성공")
	void logout_success() {
		// given
		UserPrincipal principal = UserPrincipal.builder()
			.id(1L)
			.email("logout@test.com")
			.role("ROLE_MEMBER")
			.build();

		// when
		authService.logout(principal);

		// then
		verify(refreshTokenRepository).deleteById("logout@test.com");
	}
}
