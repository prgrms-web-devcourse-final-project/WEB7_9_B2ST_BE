package com.back.b2st.domain.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

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
}
