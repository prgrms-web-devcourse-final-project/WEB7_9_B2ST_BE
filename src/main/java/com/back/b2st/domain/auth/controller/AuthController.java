package com.back.b2st.domain.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.auth.dto.LoginRequest;
import com.back.b2st.domain.auth.service.AuthService;
import com.back.b2st.global.jwt.dto.TokenInfo;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public ResponseEntity<TokenInfo> login(@Valid @RequestBody LoginRequest request) {
		TokenInfo tokenInfo = authService.login(request);
		return ResponseEntity.ok(tokenInfo);
	}
}
