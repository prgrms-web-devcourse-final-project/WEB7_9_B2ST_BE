package com.back.b2st.domain.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import com.back.b2st.domain.auth.dto.request.ConfirmRecoveryReq;
import com.back.b2st.domain.auth.dto.request.LoginReq;
import com.back.b2st.domain.auth.dto.request.RecoveryEmailReq;
import com.back.b2st.domain.auth.entity.RefreshToken;
import com.back.b2st.domain.auth.entity.WithdrawalRecoveryToken;
import com.back.b2st.domain.auth.error.AuthErrorCode;
import com.back.b2st.domain.auth.repository.RefreshTokenRepository;
import com.back.b2st.domain.auth.repository.WithdrawalRecoveryRepository;
import com.back.b2st.domain.email.service.EmailRateLimiter;
import com.back.b2st.domain.email.service.EmailSender;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.error.MemberErrorCode;
import com.back.b2st.domain.member.repository.MemberRepository;
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

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private EmailSender emailSender;

	@Mock
	private EmailRateLimiter rateLimiter;

	@Mock
	private WithdrawalRecoveryRepository recoveryRepository;

	@Nested
	@DisplayName("로그인")
	class LoginTest {

		@Test
		@DisplayName("성공 시 토큰 발급 및 Redis 저장")
		void success() {
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
	}

	@Nested
	@DisplayName("토큰 재발급")
	class ReissueTest {

		@Test
		@DisplayName("성공")
		void success() {
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

			RefreshToken storedToken = new RefreshToken(email, validRefreshToken, familyId, 1L);
			given(refreshTokenRepository.findById(email)).willReturn(Optional.of(storedToken));

			TokenInfo newToken = new TokenInfo("Bearer", "newAccess", "newRefresh");
			given(jwtTokenProvider.generateToken(authentication)).willReturn(newToken);

			// when
			TokenInfo result = authService.reissue(oldAccessToken, validRefreshToken);

			// then
			assertThat(result.accessToken()).isEqualTo("newAccess");
			verify(refreshTokenRepository).save(any(RefreshToken.class));
		}

		@Test
		@DisplayName("실패 - 저장된 토큰 불일치 (탈취 감지)")
		void fail_tokenMismatch() {
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
		@DisplayName("실패 - 유효하지 않은 Refresh Token")
		void fail_invalidToken() {
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
	}

	@Nested
	@DisplayName("로그아웃")
	class LogoutTest {

		@Test
		@DisplayName("성공")
		void success() {
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

	@Nested
	@DisplayName("탈퇴 복구 이메일 발송")
	class SendRecoveryEmailTest {

		@Test
		@DisplayName("성공")
		void success() {
			// given
			String email = "withdrawn@test.com";
			RecoveryEmailReq request = new RecoveryEmailReq(email);

			Member member = createWithdrawnMember(1L, email);
			given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
			willDoNothing().given(rateLimiter).checkRateLimit(email);

			// when
			authService.sendRecoveryEmail(request);

			// then
			verify(recoveryRepository).save(any(WithdrawalRecoveryToken.class));
			verify(emailSender).sendRecoveryEmail(eq(email), anyString(), anyString());
		}

		@Test
		@DisplayName("실패 - 존재하지 않는 회원")
		void fail_memberNotFound() {
			// given
			RecoveryEmailReq request = new RecoveryEmailReq("notfound@test.com");
			given(memberRepository.findByEmail("notfound@test.com")).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> authService.sendRecoveryEmail(request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
		}

		@Test
		@DisplayName("실패 - 탈퇴 상태 아님")
		void fail_notWithdrawn() {
			// given
			String email = "active@test.com";
			RecoveryEmailReq request = new RecoveryEmailReq(email);

			Member member = createActiveMember(1L, email);
			given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));

			// when & then
			assertThatThrownBy(() -> authService.sendRecoveryEmail(request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.NOT_WITHDRAWN_MEMBER);
		}

		@Test
		@DisplayName("실패 - 30일 초과")
		void fail_periodExpired() {
			// given
			String email = "expired@test.com";
			RecoveryEmailReq request = new RecoveryEmailReq(email);

			Member member = createExpiredMember(1L, email);
			given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));

			// when & then
			assertThatThrownBy(() -> authService.sendRecoveryEmail(request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.WITHDRAWAL_PERIOD_EXPIRED);
		}
	}

	@Nested
	@DisplayName("복구 확인")
	class ConfirmRecoveryTest {

		@Test
		@DisplayName("성공")
		void success() {
			// given
			String token = UUID.randomUUID().toString();
			ConfirmRecoveryReq request = new ConfirmRecoveryReq(token);

			WithdrawalRecoveryToken recoveryToken = WithdrawalRecoveryToken.builder()
				.token(token)
				.email("test@test.com")
				.memberId(1L)
				.build();

			Member member = createWithdrawnMember(1L, "test@test.com");

			given(recoveryRepository.findById(token)).willReturn(Optional.of(recoveryToken));
			given(memberRepository.findById(1L)).willReturn(Optional.of(member));

			// when
			authService.confirmRecovery(request);

			// then
			assertThat(member.isDeleted()).isFalse();
			verify(recoveryRepository).save(recoveryToken);
		}

		@Test
		@DisplayName("실패 - 토큰 없음")
		void fail_tokenNotFound() {
			// given
			String token = "invalid-token";
			ConfirmRecoveryReq request = new ConfirmRecoveryReq(token);

			given(recoveryRepository.findById(token)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> authService.confirmRecovery(request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.RECOVERY_TOKEN_NOT_FOUND);
		}
	}

	// 헬퍼 메서드
	private Member createActiveMember(Long id, String email) {
		Member member = Member.builder()
			.email(email)
			.password("encoded")
			.name("테스트")
			.phone("01012345678")
			.birth(LocalDate.of(1990, 1, 1))
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(false)
			.build();
		ReflectionTestUtils.setField(member, "id", id);
		return member;
	}

	private Member createWithdrawnMember(Long id, String email) {
		Member member = createActiveMember(id, email);
		member.softDelete();
		return member;
	}

	private Member createExpiredMember(Long id, String email) {
		Member member = createActiveMember(id, email);
		member.softDelete();
		ReflectionTestUtils.setField(member, "deletedAt", LocalDateTime.now().minusDays(35));
		return member;
	}
}

