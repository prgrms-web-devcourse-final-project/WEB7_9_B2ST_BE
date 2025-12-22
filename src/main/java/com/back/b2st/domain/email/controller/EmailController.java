package com.back.b2st.domain.email.controller;

import org.springframework.web.bind.annotation.PostMapping;
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
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Tag(name = "Email", description = "이메일 인증 API")
public class EmailController {

	private final EmailService emailService;

	@PostMapping("/check-duplicate")
	@Operation(summary = "이메일 중복 확인")
	public BaseResponse<CheckDuplicateRes> checkDuplicate(@Valid @RequestBody CheckDuplicateReq request) {
		return BaseResponse.success(emailService.checkDuplicate(request));
	}

	@PostMapping("/verification")
	@Operation(summary = "인증 코드 발송", description = "입력한 이메일로 6자리 인증 코드 발송")
	public BaseResponse<Void> sendVerificationCode(@Valid @RequestBody SenderVerificationReq request) {
		emailService.sendVerificationCode(request);
		return BaseResponse.success(null);
	}

	@PostMapping("/verify")
	@Operation(summary = "인증 코드 검증", description = "발송된 인증 코드 검증")
	public BaseResponse<Void> verifyCode(@Valid @RequestBody VerifyCodeReq request) {
		emailService.verifyCode(request);
		return BaseResponse.success(null);
	}
}
