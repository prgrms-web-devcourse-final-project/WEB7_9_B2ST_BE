package com.back.b2st.domain.email.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.email.dto.request.CheckDuplicateReq;
import com.back.b2st.domain.email.dto.request.SenderVerificationReq;
import com.back.b2st.domain.email.dto.request.VerifyCodeReq;
import com.back.b2st.domain.email.dto.response.CheckDuplicateRes;
import com.back.b2st.domain.email.service.EmailService;
import com.back.b2st.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Tag(name = "Email", description = "이메일 인증 API")
public class EmailController {

	private final EmailService emailService;

	// DB 조회 최적화(existsBy) + 탈퇴 회원 제외 로직
	@PostMapping("/check-duplicate")
	@Operation(summary = "이메일 중복 확인")
	public BaseResponse<CheckDuplicateRes> checkDuplicate(@Valid @RequestBody CheckDuplicateReq request) {
		return BaseResponse.success(emailService.checkDuplicate(request));
	}

	// SecureRandom(6자리) + Redis(TTL 5분) + Rate Limiting(시간당 5회) + 비동기 발송(@Async) + Thymeleaf 템플릿
	@PostMapping("/verification")
	@Operation(summary = "인증 코드 발송", description = "입력한 이메일로 6자리 인증 코드 발송")
	public BaseResponse<Void> sendVerificationCode(@Valid @RequestBody SenderVerificationReq request) {
		emailService.sendVerificationCode(request);
		return BaseResponse.success(null);
	}

	// 시도 횟수 제한(5회) + Redis 원자적 업데이트 + 성공 시 삭제 + 회원 상태 갱신
	@PostMapping("/verify")
	@Operation(summary = "인증 코드 검증", description = "발송된 인증 코드 검증")
	public BaseResponse<Void> verifyCode(@Valid @RequestBody VerifyCodeReq request) {
		emailService.verifyCode(request);
		return BaseResponse.success(null);
	}
}
