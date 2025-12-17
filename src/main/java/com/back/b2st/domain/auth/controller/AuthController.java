package com.back.b2st.domain.auth.controller;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.auth.dto.request.LoginReq;
import com.back.b2st.domain.auth.dto.request.TokenReissueReq;
import com.back.b2st.domain.auth.error.AuthErrorCode;
import com.back.b2st.domain.auth.service.AuthService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.global.error.exception.BusinessException;
import com.back.b2st.global.jwt.dto.response.TokenInfo;
import com.back.b2st.security.UserPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public BaseResponse<TokenInfo> login(@Valid @RequestBody LoginReq request, HttpServletResponse response) {
		TokenInfo tokenInfo = authService.login(request);

		ResponseCookie cookie = ResponseCookie.from("refreshToken", tokenInfo.refreshToken())
			.httpOnly(true)
			.secure(true)
			.path("/")
			.maxAge(Duration.ofDays(7))
			.sameSite("None")
			.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

		return BaseResponse.success(tokenInfo);
	}

	@PostMapping("/reissue")
	public BaseResponse<TokenInfo> reissue(
		@CookieValue(name = "refreshToken", required = false) String refreshToken,
		@RequestBody(required = false) TokenReissueReq requestBody,
		HttpServletRequest request,
		HttpServletResponse response) {

		if (refreshToken == null) {
			throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
		}

		String accessToken = (requestBody != null) ? requestBody.accessToken() : resolveToken(request);

		if (accessToken == null) {
			throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
		}

		TokenInfo tokenInfo = authService.reissue(accessToken, refreshToken);

		ResponseCookie cookie = ResponseCookie.from("refreshToken", tokenInfo.refreshToken())
			.httpOnly(true)
			.secure(true)
			.path("/")
			.maxAge(Duration.ofDays(7))
			.sameSite("None")
			.build();

		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
		return BaseResponse.success(tokenInfo);
	}

	@PostMapping("/logout")
	public BaseResponse<Void> logout(@CurrentUser UserPrincipal userPrincipal, HttpServletResponse response) {
		authService.logout(userPrincipal);

		ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
			.maxAge(0)  // 즉시 만료
			.path("/")
			.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

		return BaseResponse.success(null);
	}

	// Util로 뺄까 고민중
	private String resolveToken(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}
}
