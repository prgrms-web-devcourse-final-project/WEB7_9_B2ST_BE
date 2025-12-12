package com.back.b2st.domain.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.auth.dto.LoginRequest;
import com.back.b2st.domain.auth.dto.TokenReissueRequest;
import com.back.b2st.domain.auth.service.AuthService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.global.jwt.dto.TokenInfo;
import com.back.b2st.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public ResponseEntity<BaseResponse<TokenInfo>> login(@Valid @RequestBody LoginRequest request) {
		TokenInfo tokenInfo = authService.login(request);
		return ResponseEntity.ok(BaseResponse.success(tokenInfo));
	}

	@PostMapping("/reissue")
	public ResponseEntity<BaseResponse<TokenInfo>> reissue(@Valid @RequestBody TokenReissueRequest request) {
		TokenInfo tokenInfo = authService.reissue(request);
		return ResponseEntity.ok(BaseResponse.success(tokenInfo));
	}

	@PostMapping("/logout")
	public ResponseEntity<BaseResponse<Void>> logout(@CurrentUser UserPrincipal userPrincipal) {
		authService.logout(userPrincipal);
		return ResponseEntity.ok(BaseResponse.success(null));
	}
}
