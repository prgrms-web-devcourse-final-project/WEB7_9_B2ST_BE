package com.back.b2st.domain.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.auth.dto.request.LoginReq;
import com.back.b2st.domain.auth.dto.request.TokenReissueReq;
import com.back.b2st.domain.auth.service.AuthService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.global.jwt.dto.response.TokenInfo;
import com.back.b2st.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public BaseResponse<TokenInfo> login(@Valid @RequestBody LoginReq request) {
		TokenInfo tokenInfo = authService.login(request);
		return BaseResponse.success(tokenInfo);
	}

	@PostMapping("/reissue")
	public BaseResponse<TokenInfo> reissue(@Valid @RequestBody TokenReissueReq request) {
		TokenInfo tokenInfo = authService.reissue(request);
		return BaseResponse.success(tokenInfo);
	}

	@PostMapping("/logout")
	public BaseResponse<Void> logout(@CurrentUser UserPrincipal userPrincipal) {
		authService.logout(userPrincipal);
		return BaseResponse.success(null);
	}
}
