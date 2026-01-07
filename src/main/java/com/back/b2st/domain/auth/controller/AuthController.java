package com.back.b2st.domain.auth.controller;

import java.util.List;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.auth.dto.request.ConfirmRecoveryReq;
import com.back.b2st.domain.auth.dto.request.KakaoLoginReq;
import com.back.b2st.domain.auth.dto.request.LoginReq;
import com.back.b2st.domain.auth.dto.request.RecoveryEmailReq;
import com.back.b2st.domain.auth.dto.request.TokenReissueReq;
import com.back.b2st.domain.auth.dto.response.KakaoAuthorizeUrlRes;
import com.back.b2st.domain.auth.error.AuthErrorCode;
import com.back.b2st.domain.auth.service.AuthService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.global.error.exception.BusinessException;
import com.back.b2st.global.jwt.dto.response.TokenInfo;
import com.back.b2st.global.util.CookieUtils;
import com.back.b2st.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private static final List<String> IP_HEADERS = List.of(
		"X-Forwarded-For", // 프록시나 로드밸런서 뒤에 있을 때 원래 클라이언트 IP
		"X-Real-IP", // Nginx 등에서 설정하는 실제 클라이언트 IP
		"Proxy-Client-IP", // Apache 프록시
		"WL-Proxy-Client-IP" // WebLogic 프록시
	);
	private final AuthService authService;

	/**
	 * 로그인 처리 - Spring Security + JWT + Redis(Refresh Token) + Cookie(HttpOnly, Secure, SameSite)
	 *
	 * @param request     로그인 요청 정보
	 * @param httpRequest HTTP 요청 (IP 추출용)
	 * @param response    HTTP 응답 (쿠키 설정용)
	 */
	@PostMapping("/login")
	public BaseResponse<TokenInfo> login(
		@Valid @RequestBody LoginReq request,
		HttpServletRequest httpRequest,
		HttpServletResponse response) {
		String clientIp = getClientIp(httpRequest);
		TokenInfo tokenInfo = authService.login(request, clientIp);
		CookieUtils.setRefreshTokenCookie(response, tokenInfo.refreshToken());
		return BaseResponse.success(tokenInfo);
	}

	/**
	 * 카카오 로그인 - OIDC + RSA 서명 검증(JWKS 캐싱) + nonce 검증(Redis) + 자동 계정 연동 + 닉네임 정제
	 *
	 * @param request  카카오 로그인 요청 (인가코드)
	 * @param response HTTP 응답 (쿠키 설정용)
	 */
	@PostMapping("/kakao")
	@Operation(summary = "카카오 로그인", description = "카카오 인가 코드로 로그인 또는 회원가입 처리")
	public BaseResponse<TokenInfo> kakaoLogin(
		@Valid @RequestBody KakaoLoginReq request,
		HttpServletResponse response) {
		TokenInfo tokenInfo = authService.kakaoLogin(request);
		CookieUtils.setRefreshTokenCookie(response, tokenInfo.refreshToken());
		return BaseResponse.success(tokenInfo);
	}

	/**
	 * 카카오 콜백 (테스트용) - 백엔드 콜백 테스트용 (내부적으로 kakaoLogin 호출)
	 *
	 * @param code     카카오 인가코드
	 * @param state    CSRF 방지용 state
	 * @param response HTTP 응답
	 * @return JWT 토큰 정보
	 */
	@GetMapping("/kakao/callback")
	@Operation(summary = "카카오 콜백 (테스트용)", description = "Swagger/브라우저에서 직접 테스트 시 사용")
	public BaseResponse<TokenInfo> kakaoCallback(
		@RequestParam String code,
		@RequestParam(required = false) String state,
		HttpServletResponse response) {
		return kakaoLogin(new KakaoLoginReq(code, state), response);
	}

	/**
	 * 카카오 계정 연동 - OIDC + 기존 회원 연동 검증 + 중복 연동 방지
	 *
	 * @param userPrincipal 현재 로그인한 사용자 정보
	 * @param request       카카오 로그인 요청 (인가코드)
	 */
	@PostMapping("/link/kakao")
	@Operation(summary = "카카오 계정 연동", description = "로그인한 회원에 카카오 계정 연동")
	public BaseResponse<Void> linkKakao(
		@CurrentUser UserPrincipal userPrincipal,
		@Valid @RequestBody KakaoLoginReq request) {
		authService.linkKakaoAccount(userPrincipal.getId(), request);
		return BaseResponse.success(null);
	}

	/**
	 * 카카오 로그인 URL 생성 - nonce/state 생성 + Redis 저장(TTL 5분) + URL 빌딩
	 *
	 * @return 카카오 로그인 URL 정보
	 */
	@GetMapping("/kakao/authorize-url")
	@Operation(summary = "카카오 로그인 URL 생성", description = "nonce 포함된 카카오 로그인 URL 반환")
	public BaseResponse<KakaoAuthorizeUrlRes> getKakaoAuthorizeUrl() {
		KakaoAuthorizeUrlRes response = authService.generateKakaoAuthorizeUrl();
		return BaseResponse.success(response);
	}

	/**
	 * 토큰 재발급 - Refresh Token Rotation + 토큰 탈취 감지(Family/Generation) + Redis 갱신
	 *
	 * @param refreshToken 리프레시 토큰 (쿠키에서 추출)
	 * @param requestBody  액세스 토큰 (요청 바디)
	 * @param request      HTTP 요청
	 * @param response     HTTP 응답 (쿠키 설정용)
	 */
	@PostMapping("/reissue")
	public BaseResponse<TokenInfo> reissue(
		@CookieValue(name = "refreshToken", required = false) String refreshToken,
		@RequestBody(required = false) TokenReissueReq requestBody,
		HttpServletRequest request,
		HttpServletResponse response) {
		if (refreshToken == null) {
			throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
		}

		// 요청 바디 우선, 없으면 헤더에서 Access Token 추출
		String accessToken = (requestBody != null) ? requestBody.accessToken() : resolveToken(request);

		if (accessToken == null) {
			throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
		}

		TokenInfo tokenInfo = authService.reissue(accessToken, refreshToken);
		CookieUtils.setRefreshTokenCookie(response, tokenInfo.refreshToken());
		return BaseResponse.success(tokenInfo);
	}

	/**
	 * 로그아웃 - Redis 토큰 삭제 + 쿠키 즉시 만료 처리
	 *
	 * @param userPrincipal 현재 로그인한 사용자 정보
	 * @param response      HTTP 응답 (쿠키 삭제용)
	 */
	@PostMapping("/logout")
	public BaseResponse<Void> logout(
		@CurrentUser UserPrincipal userPrincipal,
		HttpServletResponse response) {
		authService.logout(userPrincipal);
		CookieUtils.clearRefreshTokenCookie(response);
		return BaseResponse.success(null);
	}

	/**
	 * 탈퇴 회원 복구 이메일 발송 - Rate Limiting + 복구 토큰(UUID) + Redis(TTL 24시간) + 비동기 이메일 발송
	 *
	 * @param request 복구 이메일 요청 정보
	 */
	@PostMapping("/withdrawal-recovery")
	@Operation(summary = "탈퇴 회원 복구 이메일 발송", description = "입력한 이메일로 계정 복구 링크 발송")
	public BaseResponse<Void> sendRecoveryEmail(
		@Valid @RequestBody RecoveryEmailReq request) {
		authService.sendRecoveryEmail(request);
		return BaseResponse.success(null);
	}

	/**
	 * 계정 복구 확인 - 1회용 토큰 검증 + Soft Delete 해제
	 *
	 * @param request 복구 확인 요청 정보
	 */
	@PostMapping("/confirm-recovery")
	@Operation(summary = "계정 복구 확인", description = "발송된 링크 접속 시 해당 api로 검증")
	public BaseResponse<Void> confirmRecovery(
		@Valid @RequestBody ConfirmRecoveryReq request) {
		authService.confirmRecovery(request);
		return BaseResponse.success(null);
	}

	// 밑으로 유틸 메서드

	// 여러 헤더에서 클라이언트 IP 추출 로직
	private String getClientIp(HttpServletRequest httpRequest) {
		return IP_HEADERS.stream()
			.map(httpRequest::getHeader)
			.filter(StringUtils::hasText)
			.map(ip -> ip.split(",")[0].trim()) // 여러 IP가 있을 경우 첫 번째 IP 사용
			.findFirst()
			.orElseGet(httpRequest::getRemoteAddr);
	}

	// Authorization 헤더에서 Bearer 토큰 추출 로직
	private String resolveToken(
		HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}
}
