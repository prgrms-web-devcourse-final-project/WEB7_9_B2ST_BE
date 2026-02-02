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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import com.back.b2st.domain.auth.client.KakaoApiClient;
import com.back.b2st.domain.auth.dto.oauth.KakaoIdTokenPayload;
import com.back.b2st.domain.auth.dto.request.ConfirmRecoveryReq;
import com.back.b2st.domain.auth.dto.request.KakaoLoginReq;
import com.back.b2st.domain.auth.dto.request.LoginReq;
import com.back.b2st.domain.auth.dto.request.RecoveryEmailReq;
import com.back.b2st.domain.auth.entity.RefreshToken;
import com.back.b2st.domain.auth.entity.WithdrawalRecoveryToken;
import com.back.b2st.domain.auth.error.AuthErrorCode;
import com.back.b2st.domain.auth.metrics.AuthMetrics;
import com.back.b2st.domain.auth.repository.OAuthNonceRepository;
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
import com.back.b2st.security.CustomUserDetails;
import com.back.b2st.security.UserPrincipal;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final String TEST_CLIENT_IP = "127.0.0.1";

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

	@Mock
	private KakaoApiClient kakaoApiClient;

	@Mock
	private OAuthNonceRepository nonceRepository;

	@Mock
	private LoginSecurityService loginSecurityService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Mock
	private AuthMetrics authMetrics;

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

	private Member createKakaoMember(Long id, String email, String kakaoId) {
		Member member = Member.builder()
			.email(email)
			.password(null)
			.name("카카오유저")
			.phone(null)
			.birth(null)
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.KAKAO)
			.providerId(kakaoId)
			.isEmailVerified(true)
			.isIdentityVerified(false)
			.build();
		ReflectionTestUtils.setField(member, "id", id);
		return member;
	}

	private KakaoIdTokenPayload createKakaoIdTokenPayload(Long kakaoId, String email, String nickname) {
		return new KakaoIdTokenPayload(
			"https://kauth.kakao.com", // iss
			"test-client-id", // aud
			String.valueOf(kakaoId), // sub
			System.currentTimeMillis() / 1000, // iat
			(System.currentTimeMillis() / 1000) + 3600, // exp
			System.currentTimeMillis() / 1000, // auth_time
			null, // nonce
			nickname,
			null, // picture
			email);
	}

	private KakaoIdTokenPayload createKakaoIdTokenPayloadWithNonce(
		Long kakaoId, String email, String nickname, String nonce) {
		return new KakaoIdTokenPayload(
			"https://kauth.kakao.com",
			"test-client-id",
			String.valueOf(kakaoId),
			System.currentTimeMillis() / 1000,
			(System.currentTimeMillis() / 1000) + 3600,
			System.currentTimeMillis() / 1000,
			nonce, // nonce 포함
			nickname,
			null,
			email);
	}

	@Nested
	@DisplayName("로그인")
	class LoginTest {

		@Test
		@DisplayName("성공 시 토큰 발급 및 Redis 저장")
		void success() {
			// given
			LoginReq request = new LoginReq("test@test.com", "Password123!");

			Member member = createActiveMember(1L, "test@test.com");
			CustomUserDetails userDetails = new CustomUserDetails(member);

			AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
			Authentication authentication = mock(Authentication.class);

			given(authenticationManagerBuilder.getObject()).willReturn(authenticationManager);
			given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.willReturn(authentication);
			given(authentication.getPrincipal()).willReturn(userDetails);

			TokenInfo expectedToken = new TokenInfo("Bearer", "access", "refresh");
			given(jwtTokenProvider.generateToken(any(Authentication.class))).willReturn(expectedToken);

			// when
			TokenInfo result = authService.login(request, TEST_CLIENT_IP);

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

	@Nested
	@DisplayName("카카오 로그인")
	class KakaoLoginTest {

		@Test
		@DisplayName("성공 - 신규 회원 생성 후 토큰 발급")
		void success_newMember() {
			// given
			String code = "test-authorization-code";
			KakaoLoginReq request = new KakaoLoginReq(code, null);

			KakaoIdTokenPayload payload = createKakaoIdTokenPayload(
				123456789L, "newuser@kakao.com", "카카오유저");

			given(kakaoApiClient.getTokenAndParseIdToken(code)).willReturn(payload);
			given(memberRepository.findByProviderId("123456789")).willReturn(Optional.empty());
			given(memberRepository.findByEmail("newuser@kakao.com")).willReturn(Optional.empty());
			given(memberRepository.save(any(Member.class))).willAnswer(invocation -> {
				Member saved = invocation.getArgument(0);
				ReflectionTestUtils.setField(saved, "id", 1L);
				return saved;
			});
			given(jwtTokenProvider.generateToken(any())).willReturn(
				new TokenInfo("Bearer", "access-token", "refresh-token"));

			// when
			TokenInfo result = authService.kakaoLogin(request);

			// then
			assertThat(result.accessToken()).isEqualTo("access-token");
			verify(memberRepository).save(any(Member.class));
			verify(refreshTokenRepository).save(any(RefreshToken.class));
		}

		@Test
		@DisplayName("성공 - 기존 연동 회원 로그인")
		void success_existingLinkedMember() {
			// given
			String code = "test-code";
			KakaoLoginReq request = new KakaoLoginReq(code, null);

			KakaoIdTokenPayload payload = createKakaoIdTokenPayload(
				123456789L, "existing@kakao.com", "기존유저");

			Member existingMember = createKakaoMember(1L, "existing@kakao.com", "123456789");

			given(kakaoApiClient.getTokenAndParseIdToken(code)).willReturn(payload);
			given(memberRepository.findByProviderId("123456789")).willReturn(Optional.of(existingMember));
			given(jwtTokenProvider.generateToken(any())).willReturn(
				new TokenInfo("Bearer", "access-token", "refresh-token"));

			// when
			TokenInfo result = authService.kakaoLogin(request);

			// then
			assertThat(result.accessToken()).isEqualTo("access-token");
			verify(memberRepository, never()).save(any(Member.class)); // 저장 안 함
		}

		@Test
		@DisplayName("성공 - 이메일 가입 회원에 카카오 자동 연동")
		void success_autoLinkToEmailMember() {
			// given
			String code = "test-code";
			KakaoLoginReq request = new KakaoLoginReq(code, null);

			KakaoIdTokenPayload payload = createKakaoIdTokenPayload(
				123456789L, "email@test.com", "이메일유저");

			Member emailMember = createActiveMember(1L, "email@test.com");

			given(kakaoApiClient.getTokenAndParseIdToken(code)).willReturn(payload);
			given(memberRepository.findByProviderId("123456789")).willReturn(Optional.empty());
			given(memberRepository.findByEmail("email@test.com")).willReturn(Optional.of(emailMember));
			given(memberRepository.save(emailMember)).willReturn(emailMember);
			given(jwtTokenProvider.generateToken(any())).willReturn(
				new TokenInfo("Bearer", "access-token", "refresh-token"));

			// when
			TokenInfo result = authService.kakaoLogin(request);

			// then
			assertThat(result.accessToken()).isEqualTo("access-token");
			assertThat(emailMember.getProviderId()).isEqualTo("123456789"); // 연동됨
			verify(memberRepository).save(emailMember);
		}

		@Test
		@DisplayName("실패 - 이메일 미제공")
		void fail_emailNotProvided() {
			// given
			String code = "test-code";
			KakaoLoginReq request = new KakaoLoginReq(code, null);

			KakaoIdTokenPayload payload = createKakaoIdTokenPayload(
				123456789L, null, "유저" // 이메일 없음
			);

			given(kakaoApiClient.getTokenAndParseIdToken(code)).willReturn(payload);

			// when & then
			assertThatThrownBy(() -> authService.kakaoLogin(request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.OAUTH_EMAIL_NOT_PROVIDED);
		}

		@Test
		@DisplayName("실패 - 탈퇴 회원")
		void fail_withdrawnMember() {
			// given
			String code = "test-code";
			KakaoLoginReq request = new KakaoLoginReq(code, null);

			KakaoIdTokenPayload payload = createKakaoIdTokenPayload(
				123456789L, "withdrawn@kakao.com", "탈퇴유저");

			Member withdrawnMember = createWithdrawnMember(1L, "withdrawn@kakao.com");
			ReflectionTestUtils.setField(withdrawnMember, "providerId", "123456789");
			ReflectionTestUtils.setField(withdrawnMember, "provider", Member.Provider.KAKAO);

			given(kakaoApiClient.getTokenAndParseIdToken(code)).willReturn(payload);
			given(memberRepository.findByProviderId("123456789")).willReturn(Optional.of(withdrawnMember));

			// when & then
			assertThatThrownBy(() -> authService.kakaoLogin(request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(MemberErrorCode.ALREADY_WITHDRAWN);
		}
	}

	@Nested
	@DisplayName("카카오 계정 연동")
	class LinkKakaoAccountTest {

		@Test
		@DisplayName("성공")
		void success() {
			// given
			Long memberId = 1L;
			String code = "test-code";
			KakaoLoginReq request = new KakaoLoginReq(code, null);

			KakaoIdTokenPayload payload = createKakaoIdTokenPayload(
				123456789L, "user@kakao.com", "유저");

			Member member = createActiveMember(memberId, "user@test.com");

			given(kakaoApiClient.getTokenAndParseIdToken(code)).willReturn(payload);
			given(memberRepository.findByProviderId("123456789")).willReturn(Optional.empty());
			given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

			// when
			authService.linkKakaoAccount(memberId, request);

			// then
			assertThat(member.getProviderId()).isEqualTo("123456789");
		}

		@Test
		@DisplayName("실패 - 이미 다른 회원에 연동됨")
		void fail_alreadyLinkedToOther() {
			// given
			Long memberId = 1L;
			String code = "test-code";
			KakaoLoginReq request = new KakaoLoginReq(code, null);

			KakaoIdTokenPayload payload = createKakaoIdTokenPayload(
				123456789L, "other@kakao.com", "다른유저");

			Member otherMember = createKakaoMember(2L, "other@test.com", "123456789");

			given(kakaoApiClient.getTokenAndParseIdToken(code)).willReturn(payload);
			given(memberRepository.findByProviderId("123456789")).willReturn(Optional.of(otherMember));

			// when & then
			assertThatThrownBy(() -> authService.linkKakaoAccount(memberId, request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.OAUTH_ALREADY_LINKED);
		}
	}

	@Nested
	@DisplayName("nonce 검증")
	class ValidateNonceTest {

		@Test
		@DisplayName("성공 - 유효한 nonce로 로그인")
		void success_validNonce() {
			// given
			String code = "test-code";
			String nonce = UUID.randomUUID().toString();
			KakaoLoginReq request = new KakaoLoginReq(code, null);

			KakaoIdTokenPayload payload = createKakaoIdTokenPayloadWithNonce(
				123456789L, "user@kakao.com", "유저", nonce);

			given(kakaoApiClient.getTokenAndParseIdToken(code)).willReturn(payload);
			given(nonceRepository.existsById(nonce)).willReturn(true);
			given(memberRepository.findByProviderId("123456789")).willReturn(Optional.empty());
			given(memberRepository.findByEmail("user@kakao.com")).willReturn(Optional.empty());
			given(memberRepository.save(any(Member.class))).willAnswer(invocation -> {
				Member saved = invocation.getArgument(0);
				ReflectionTestUtils.setField(saved, "id", 1L);
				return saved;
			});
			given(jwtTokenProvider.generateToken(any())).willReturn(
				new TokenInfo("Bearer", "access-token", "refresh-token"));

			// when
			TokenInfo result = authService.kakaoLogin(request);

			// then
			assertThat(result.accessToken()).isEqualTo("access-token");
			verify(nonceRepository).existsById(nonce);
			verify(nonceRepository).deleteById(nonce);
		}

		@Test
		@DisplayName("실패 - 유효하지 않은 nonce")
		void fail_invalidNonce() {
			// given
			String code = "test-code";
			String invalidNonce = "invalid-nonce";
			KakaoLoginReq request = new KakaoLoginReq(code, null);

			KakaoIdTokenPayload payload = createKakaoIdTokenPayloadWithNonce(
				123456789L, "user@kakao.com", "유저", invalidNonce);

			given(kakaoApiClient.getTokenAndParseIdToken(code)).willReturn(payload);
			given(nonceRepository.existsById(invalidNonce)).willReturn(false);

			// when & then
			assertThatThrownBy(() -> authService.kakaoLogin(request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
		}

		@Test
		@DisplayName("성공 - nonce 없으면 레거시 호환 (경고만)")
		void success_noNonce_legacyCompatibility() {
			// given
			String code = "test-code";
			KakaoLoginReq request = new KakaoLoginReq(code, null);

			KakaoIdTokenPayload payload = createKakaoIdTokenPayload(
				123456789L, "user@kakao.com", "유저" // nonce = null
			);

			given(kakaoApiClient.getTokenAndParseIdToken(code)).willReturn(payload);
			given(memberRepository.findByProviderId("123456789")).willReturn(Optional.empty());
			given(memberRepository.findByEmail("user@kakao.com")).willReturn(Optional.empty());
			given(memberRepository.save(any(Member.class))).willAnswer(invocation -> {
				Member saved = invocation.getArgument(0);
				ReflectionTestUtils.setField(saved, "id", 1L);
				return saved;
			});
			given(jwtTokenProvider.generateToken(any())).willReturn(
				new TokenInfo("Bearer", "access-token", "refresh-token"));

			// when
			TokenInfo result = authService.kakaoLogin(request);

			// then
			assertThat(result).isNotNull();
			verify(nonceRepository, never()).existsById(any());
		}
	}

	@Nested
	@DisplayName("카카오 로그인 URL 생성")
	class GenerateKakaoAuthorizeUrlTest {

		@Test
		@DisplayName("성공 - URL에 필수 파라미터 포함")
		void success() {
			// given
			ReflectionTestUtils.setField(authService, "kakaoClientId", "test-client-id");
			ReflectionTestUtils.setField(authService, "kakaoRedirectUri", "http://localhost:8080/callback");

			// when
			var result = authService.generateKakaoAuthorizeUrl();

			// then
			assertThat(result.authorizeUrl()).contains("client_id=test-client-id");
			assertThat(result.authorizeUrl()).contains("redirect_uri=http://localhost:8080/callback");
			assertThat(result.authorizeUrl()).contains("response_type=code");
			assertThat(result.authorizeUrl()).contains("scope=openid");
			assertThat(result.authorizeUrl()).contains("nonce=");
			assertThat(result.authorizeUrl()).contains("state=");
			assertThat(result.state()).isNotBlank();
			assertThat(result.nonce()).isNotBlank();

			// nonce Redis 저장 확인
			verify(nonceRepository).save(any());
		}
	}
}
