import static org.assertj.core.api.Assertions.*;
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

import com.back.b2st.domain.auth.Error.AuthErrorCode;
import com.back.b2st.domain.auth.dto.LoginRequest;
import com.back.b2st.domain.auth.dto.TokenReissueRequest;
import com.back.b2st.domain.auth.entity.RefreshToken;
import com.back.b2st.domain.auth.repository.RefreshTokenRepository;
import com.back.b2st.domain.auth.service.AuthService;
import com.back.b2st.global.error.exception.BusinessException;
import com.back.b2st.global.jwt.JwtTokenProvider;
import com.back.b2st.global.jwt.dto.TokenInfo;
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
		LoginRequest request = new LoginRequest();
		ReflectionTestUtils.setField(request, "email", "test@test.com");
		ReflectionTestUtils.setField(request, "password", "Password123!");

		AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
		Authentication authentication = mock(Authentication.class);

		given(authenticationManagerBuilder.getObject()).willReturn(authenticationManager);
		given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.willReturn(authentication);
		given(authentication.getName()).willReturn("test@test.com");

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
		verify(refreshTokenRepository).save(any(RefreshToken.class));
	}

	@Test
	@DisplayName("토큰 재발급 성공")
	void reissue_success() {
		// given
		TokenReissueRequest request = TokenReissueRequest.builder()
			.accessToken("oldAccess")
			.refreshToken("validRefresh")
			.build();

		Authentication authentication = mock(Authentication.class);
		UserPrincipal principal = UserPrincipal.builder().email("test@test.com").id(1L).build();
		given(authentication.getPrincipal()).willReturn(principal);

		// Refresh Token 검증
		willDoNothing().given(jwtTokenProvider).validateToken("validRefresh");

		// Access Token 서명 검증
		given(jwtTokenProvider.validateTokenSignature("oldAccess")).willReturn(true);
		given(jwtTokenProvider.getAuthentication("oldAccess")).willReturn(authentication);
		RefreshToken storedToken = new RefreshToken("test@test.com", "validRefresh");
		given(refreshTokenRepository.findById("test@test.com")).willReturn(java.util.Optional.of(storedToken));

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
		verify(refreshTokenRepository).save(any(RefreshToken.class));
	}

	@Test
	@DisplayName("토큰 재발급 실패 - 저장된 토큰 불일치")
	void reissue_fail_token_mismatch() {
		// given
		TokenReissueRequest request = TokenReissueRequest.builder()
			.accessToken("oldAccess")
			.refreshToken("hackRefresh")
			.build();

		Authentication authentication = mock(Authentication.class);
		UserPrincipal principal = UserPrincipal.builder().email("test@test.com").id(1L).build();
		given(authentication.getPrincipal()).willReturn(principal);

		// validateToken은 성공한다고 가정
		willDoNothing().given(jwtTokenProvider).validateToken("hackRefresh");
		given(jwtTokenProvider.validateTokenSignature("oldAccess")).willReturn(true);
		given(jwtTokenProvider.getAuthentication("oldAccess")).willReturn(authentication);

		// Redis에는 "originRefresh"가 저장되어 있음 (요청은 hackRefresh)
		RefreshToken storedToken = new RefreshToken("test@test.com", "originRefresh");
		given(refreshTokenRepository.findById("test@test.com")).willReturn(java.util.Optional.of(storedToken));

		// when & then
		assertThatThrownBy(() -> authService.reissue(request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(AuthErrorCode.TOKEN_MISMATCH);
	}

	@Test
	@DisplayName("토큰 재발급 실패 - 유효하지 않은 Refresh Token (예외 발생)")
	void reissue_fail_invalid_refresh_token() {
		// given
		TokenReissueRequest request = TokenReissueRequest.builder()
			.accessToken("oldAccess")
			.refreshToken("invalidRefresh")
			.build();

		// AuthService에서 catch해서 BusinessException(INVALID_REFRESH_TOKEN)으로 감싸는지 확인
		willThrow(new io.jsonwebtoken.security.SignatureException("Invalid signature"))
			.given(jwtTokenProvider).validateToken("invalidRefresh");

		// when & then
		assertThatThrownBy(() -> authService.reissue(request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
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