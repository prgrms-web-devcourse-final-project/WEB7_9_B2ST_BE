package com.back.b2st.domain.auth.service;

import static com.back.b2st.global.util.MaskingUtil.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.auth.client.KakaoApiClient;
import com.back.b2st.domain.auth.dto.oauth.KakaoIdTokenPayload;
import com.back.b2st.domain.auth.dto.request.ConfirmRecoveryReq;
import com.back.b2st.domain.auth.dto.request.KakaoLoginReq;
import com.back.b2st.domain.auth.dto.request.LoginReq;
import com.back.b2st.domain.auth.dto.request.RecoveryEmailReq;
import com.back.b2st.domain.auth.dto.response.KakaoAuthorizeUrlRes;
import com.back.b2st.domain.auth.dto.response.LoginEvent;
import com.back.b2st.domain.auth.entity.OAuthNonce;
import com.back.b2st.domain.auth.entity.RefreshToken;
import com.back.b2st.domain.auth.entity.WithdrawalRecoveryToken;
import com.back.b2st.domain.auth.error.AuthErrorCode;
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
import com.back.b2st.global.util.NicknameUtils;
import com.back.b2st.security.CustomUserDetails;
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
	private final MemberRepository memberRepository;
	private final EmailSender emailSender;
	private final EmailRateLimiter rateLimiter;
	private final WithdrawalRecoveryRepository recoveryRepository;
	private final KakaoApiClient kakaoApiClient;
	private final OAuthNonceRepository nonceRepository;
	private final LoginSecurityService loginSecurityService;
	private final ApplicationEventPublisher eventPublisher;
	@Value("${oauth.kakao.client-id}")
	private String kakaoClientId;
	@Value("${oauth.kakao.redirect-uri}")
	private String kakaoRedirectUri;
	@Value("${oauth.kakao.default-nickname:카카오사용자}")
	private String defaultKakaoNickname;

	/**
	 * 로그인 처리
	 * - Rate Limiting + 계정 잠금 검사
	 * - Spring Security 인증
	 * - JWT 발급
	 *
	 * @param request  로그인 요청
	 * @param clientIp 클라이언트 IP (감사 로깅용)
	 * @return JWT 토큰 정보
	 */
	@Transactional
	public TokenInfo login(LoginReq request, String clientIp) {
		String email = request.email();

		// 계정 잠금 확인
		loginSecurityService.checkAccountLock(email);

		try {
			// 인증
			Authentication authentication = authenticateWithEmailPassword(request);
			Member member = extractMemberFromAuthentication(authentication);

			// 로그인 성공 처리(시도 횟수 리셋)
			loginSecurityService.onLoginSuccess(email, clientIp);
			eventPublisher.publishEvent(LoginEvent.success(email, clientIp));

			// JWT 발급
			return generateTokenForMember(member);
		} catch (BusinessException e) {
			// 로그인 실패 처리(시도 횟수 증가)
			loginSecurityService.recordFailedAttempt(email, clientIp);
			eventPublisher.publishEvent(LoginEvent.failure(email, clientIp, e.getMessage(), e.getErrorCode()));
			throw e;
		} catch (Exception e) {
			// 기타 예외 처리
			loginSecurityService.recordFailedAttempt(email, clientIp);
			eventPublisher.publishEvent(LoginEvent.failure(email, clientIp, e.getMessage(), null));
			throw e;
		}
	}

	/**
	 * 카카오 로그인 - OIDC ID Token 파싱 + nonce 검증 + 자동 계정 연동 + 닉네임 정제
	 *
	 * @param request 카카오 로그인 요청 (인가코드)
	 * @return JWT 토큰 정보
	 */
	@Transactional
	public TokenInfo kakaoLogin(KakaoLoginReq request) {
		// OIDC 호출. 액세스 토큰 발급 + 정보 조회
		KakaoIdTokenPayload payload = fetchKakaoUserInfo(request.code());

		// nonce 검증
		validateNonce(payload.nonce());

		// 검증
		validateKakaoEmail(payload);

		// 회원 처리
		Member member = findOrCreateKakaoMember(payload);
		validateNotWithdrawn(member);

		// JWT 발급
		TokenInfo tokenInfo = generateTokenForMember(member);
		log.info("[Kakao] 로그인 성공: MemberID={}, Email={}", member.getId(), maskEmail(member.getEmail()));

		return tokenInfo;
	}

	/**
	 * 카카오 계정 연동 - 카카오 ID 조회 + 중복 연동 방지 + 회원 연동
	 *
	 * @param memberId 회원 ID
	 * @param request  카카오 로그인 요청 (인가코드)
	 */
	@Transactional
	public void linkKakaoAccount(Long memberId, KakaoLoginReq request) {
		// 카카오 정보 조회
		KakaoIdTokenPayload payload = fetchKakaoUserInfo(request.code());
		String kakaoId = String.valueOf(payload.getKakaoId());

		// 검증
		validateKakaoNotLinkedToOther(kakaoId, memberId);

		// 연동
		Member member = findMemberById(memberId);
		member.linkKakao(kakaoId);

		log.info("[Kakao] 계정 연동 완료: MemberID={}, KakaoId={}", memberId, kakaoId);
	}

	/**
	 * 카카오 로그인 URL 생성 - nonce/state 생성 + Redis 저장(TTL 5분) + URL 빌딩
	 *
	 * @return 카카오 로그인 URL 정보
	 */
	public KakaoAuthorizeUrlRes generateKakaoAuthorizeUrl() {
		// 랜덤 생성
		String nonce = UUID.randomUUID().toString();
		String state = UUID.randomUUID().toString();

		// nonce redis 저장
		nonceRepository.save(OAuthNonce.create(nonce, state));

		// 카카오 로그인 URL 새성
		String authrizeUrl = String.format(
			"https://kauth.kakao.com/oauth/authorize" +
				"?client_id=%s" +
				"&redirect_uri=%s" +
				"&response_type=code" +
				"&scope=openid%%20profile_nickname%%20account_email" + // URL 인코딩
				"&nonce=%s" +
				"&state=%s",
			kakaoClientId, kakaoRedirectUri, nonce, state);

		return new KakaoAuthorizeUrlRes(authrizeUrl, state, nonce);
	}

	/**
	 * 토큰 재발급 - Refresh Token Rotation + 탈취 감지(Family/Generation) + Redis 갱신
	 *
	 * @param accessToken  액세스 토큰
	 * @param refreshToken 리프레시 토큰
	 * @return 새 JWT 토큰 정보
	 */
	@Transactional
	public TokenInfo reissue(String accessToken, String refreshToken) {
		// 검증
		validateRefreshToken(refreshToken);
		validateAccessTokenSignature(accessToken);

		// 토큰에서 이메일 추출
		String email = extractEmailFromToken(accessToken);

		// Redis 검증
		RefreshToken storedToken = findStoredRefreshToken(email);
		validateTokenNotReused(storedToken, refreshToken, email);

		// 새 토큰 발급
		Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
		TokenInfo newToken = jwtTokenProvider.generateToken(authentication);

		// Redis 업데이트
		saveRefreshToken(email, newToken.refreshToken(),
			storedToken.getFamily(), storedToken.getGeneration() + 1);

		return newToken;
	}

	/**
	 * 로그아웃 - Redis 토큰 삭제
	 *
	 * @param principal 현재 로그인한 사용자 정보
	 */
	@Transactional
	public void logout(UserPrincipal principal) {
		refreshTokenRepository.deleteById(principal.getEmail());
	}

	/**
	 * 탈퇴 회원 복구 이메일 발송 - Rate Limiting + 복구 토큰(UUID) + Redis(TTL 24시간) + 비동기 발송
	 *
	 * @param request 복구 이메일 요청 정보
	 */
	@Transactional
	public void sendRecoveryEmail(RecoveryEmailReq request) {
		String email = request.email();
		Member member = findMemberByEmail(email);

		// 검증
		validateIsWithdrawn(member);
		validateWithdrawalPeriod(member);
		rateLimiter.checkRateLimit(email);

		// 복구 토큰 생성 및 발송
		String token = createRecoveryToken(email, member.getId());
		sendRecoveryLink(email, member.getName(), token);

		log.info("복구 이메일 발송: Email={}", maskEmail(email));
	}

	/**
	 * 계정 복구 확인 - 1회용 토큰 검증 + Soft Delete 해제
	 *
	 * @param request 복구 확인 요청 정보
	 */
	@Transactional
	public void confirmRecovery(ConfirmRecoveryReq request) {
		WithdrawalRecoveryToken recoveryToken = findRecoveryToken(request.token());
		Member member = findMemberById(recoveryToken.getMemberId());

		member.cancelWithdrawal();

		log.info("계정 복구 완료: MemberID={}", member.getId());
	}

	// 이 밑으로 validate 메서드
	// 이 밑으로 validate 메서드
	// 이 밑으로 validate 메서드

	private void validateRefreshToken(String accessToken) {
		try {
			jwtTokenProvider.validateToken(accessToken);
		} catch (Exception e) {
			throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
		}
	}

	// 토큰 서명 검증
	private void validateAccessTokenSignature(String accessToken) {
		if (!jwtTokenProvider.validateTokenSignature(accessToken)) {
			throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
		}
	}

	// 카카오 이메일 검증
	// 프엔에서 재동의 유도 필요
	private void validateKakaoEmail(KakaoIdTokenPayload payload) {
		if (!payload.hasEmail()) {
			throw new BusinessException(AuthErrorCode.OAUTH_EMAIL_NOT_PROVIDED);
		}
	}

	// 탈퇴 회원이 아닌지
	private void validateNotWithdrawn(Member member) {
		if (member.isDeleted()) {
			throw new BusinessException(MemberErrorCode.ALREADY_WITHDRAWN);
		}
	}

	// 탈퇴 회원인지
	private void validateIsWithdrawn(Member member) {
		if (!member.isDeleted()) {
			throw new BusinessException(AuthErrorCode.NOT_WITHDRAWN_MEMBER);
		}
	}

	// 복구 가능 기간인지
	private void validateWithdrawalPeriod(Member member) {
		if (member.getDeletedAt().plusDays(30).isBefore(LocalDateTime.now())) {
			throw new BusinessException(AuthErrorCode.WITHDRAWAL_PERIOD_EXPIRED);
		}
	}

	// 다른 계정에 연동된 카카오 계정이 아닌지
	private void validateKakaoNotLinkedToOther(String kakaoId, Long currentMemberId) {
		memberRepository.findByProviderId(kakaoId)
			.filter(linked -> !linked.getId().equals(currentMemberId))
			.ifPresent(linked -> {
				throw new BusinessException(AuthErrorCode.OAUTH_ALREADY_LINKED);
			});
	}

	private void validateNonce(String nonce) {
		if (nonce == null || nonce.isBlank()) {
			log.warn("[Kakao] nonce 없음 - nonce 검증 건너뜀 (레거시 호환)");
			return; // TODO: p1땐 에러처리
		}

		// redis 체크
		boolean exists = nonceRepository.existsById(nonce);

		if (!exists) {
			// 보안: nonce 값 노출 방지
			log.warn("[Kakao] 유효하지 않은 nonce 감지");
			throw new BusinessException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
		}

		// nonce 삭제. 1회용
		nonceRepository.deleteById(nonce);
		log.info("[Kakao] nonce 검증 성공");
	}

	// 리프레쉬 토큰이 동일한지 검증
	private void validateTokenNotReused(RefreshToken storedToken, String providedToken, String email) {
		if (!storedToken.getToken().equals(providedToken)) {
			refreshTokenRepository.deleteById(email);
			log.warn("🚨 토큰 탈취 감지! (Token Reuse Detected) User: {}", maskEmail(email));
			throw new BusinessException(AuthErrorCode.TOKEN_REUSE_DETECTED);
		}
	}

	// 이 밑으로 find 메서드
	// 이 밑으로 find 메서드
	// 이 밑으로 find 메서드

	private Member findMemberById(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	private Member findMemberByEmail(String email) {
		return memberRepository.findByEmail(email)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
	}

	private RefreshToken findStoredRefreshToken(String email) {
		return refreshTokenRepository.findById(email)
			.orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_TOKEN));
	}

	private WithdrawalRecoveryToken findRecoveryToken(String token) {
		return recoveryRepository.findById(token)
			.orElseThrow(() -> new BusinessException(AuthErrorCode.RECOVERY_TOKEN_NOT_FOUND));
	}

	private Optional<Member> findMemberByProviderId(String providerId) {
		return memberRepository.findByProviderId(providerId);
	}

	// 이 밑으로 헬퍼 메서드
	// 이 밑으로 헬퍼 메서드
	// 이 밑으로 헬퍼 메서드

	// OIDC. 토큰이랑 사용자 정보 동시 호출
	private KakaoIdTokenPayload fetchKakaoUserInfo(String code) {
		return kakaoApiClient.getTokenAndParseIdToken(code);
	}

	// 로그인 리퀘 인증
	private Authentication authenticateWithEmailPassword(LoginReq request) {
		UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(request.email(),
			request.password());
		return authenticationManagerBuilder.getObject().authenticate(authToken);
	}

	// 인증 객체서 유저 엔티티 추출
	private Member extractMemberFromAuthentication(Authentication authentication) {
		CustomUserDetails userDetails = (CustomUserDetails)authentication.getPrincipal();
		return userDetails.getMember();
	}

	// 액세스 토큰 파싱 후 이메일 추출
	private String extractEmailFromToken(String accessToken) {
		Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
		Object principal = authentication.getPrincipal();

		if (principal instanceof UserPrincipal userPrincipal) {
			return userPrincipal.getEmail();
		}
		return authentication.getName();
	}

	// 카카오 회원 조회 또는 생성
	// 흐름: 카카오 id 조회하고 있으면 반환. 없으면 이메일로 조회 후 있으면 반환. 그것도 없으면 신규생성
	private Member findOrCreateKakaoMember(KakaoIdTokenPayload payload) {
		String email = payload.email();
		String kakaoId = String.valueOf(payload.getKakaoId());
		String nickname = payload.nickname() != null ? payload.nickname() : defaultKakaoNickname;
		// 이미 연동된 회원 확인
		Optional<Member> linkedMember = findMemberByProviderId(kakaoId);
		if (linkedMember.isPresent()) {
			log.info("[Kakao] 기존 연동 회원 로그인: MemberID={}", linkedMember.get().getId());
			return linkedMember.get();
		}
		// 이메일로 가입된 회원 확인
		return memberRepository.findByEmail(email)
			.map(existing -> linkKakaoToExistingMember(existing, kakaoId))
			.orElseGet(() -> createNewKakaoMember(email, kakaoId, nickname));
	}

	// 이메일 연동
	private Member linkKakaoToExistingMember(Member member, String kakaoId) {
		if (member.getProvider() == Member.Provider.KAKAO) { // 안올 것 같지만 혹시 몰라서
			log.debug("[Kakao] 기존 카카오 회원 로그인: MemberID={}", member.getId());
			return member;
		}
		if (member.getProviderId() == null) {
			log.info("[Kakao] 이메일 회원에 카카오 연동: MemberID={}", member.getId());
			member.linkKakao(kakaoId);
			return memberRepository.save(member);
		}
		return member;
	}

	// 신규 생성
	private Member createNewKakaoMember(String email, String kakaoId, String nickname) {

		String sanitizedNickname = NicknameUtils.sanitize(nickname, defaultKakaoNickname);

		Member member = Member.builder()
			.email(email)
			.password(null)
			.name(sanitizedNickname)
			.phone(null)
			.birth(null)
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.KAKAO)
			.providerId(kakaoId)
			.isEmailVerified(true)
			.isIdentityVerified(false)
			.build();
		Member saved = memberRepository.save(member);
		log.info("[Kakao] 신규 회원 생성: MemberID={}, KakaoID={}", saved.getId(), kakaoId);
		return saved;
	}

	// 로그인 생성
	private TokenInfo generateTokenForMember(Member member) {
		CustomUserDetails userDetails = new CustomUserDetails(member);
		UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
			userDetails, null, userDetails.getAuthorities());
		TokenInfo tokenInfo = jwtTokenProvider.generateToken(authToken);
		saveRefreshToken(member.getEmail(), tokenInfo.refreshToken(),
			UUID.randomUUID().toString(), 1L);
		return tokenInfo;
	}

	// 리프레쉬 토큰 세이브
	private void saveRefreshToken(String email, String token, String family, Long generation) {
		refreshTokenRepository.save(new RefreshToken(email, token, family, generation));
	}

	// 탈퇴 철회 토큰 생성
	private String createRecoveryToken(String email, Long memberId) {
		String token = UUID.randomUUID().toString();
		WithdrawalRecoveryToken recoveryToken = WithdrawalRecoveryToken.builder()
			.token(token)
			.email(email)
			.memberId(memberId)
			.build();
		recoveryRepository.save(recoveryToken);
		return token;
	}

	// 인증 확인 처리하는 api 호출하는 링크
	private void sendRecoveryLink(String email, String name, String token) {
		String recoveryLink = "https://doncrytt.vercel.app/recovery-withdraw?token=" + token;
		emailSender.sendRecoveryEmail(email, name, recoveryLink);
	}
}
