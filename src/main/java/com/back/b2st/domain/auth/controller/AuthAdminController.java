package com.back.b2st.domain.auth.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.auth.dto.response.LockedAccountRes;
import com.back.b2st.domain.auth.dto.response.LoginLogAdminRes;
import com.back.b2st.domain.auth.dto.response.SignupLogAdminRes;
import com.back.b2st.domain.auth.service.AuthAdminService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/auth")
@Tag(name = "AuthAdminController", description = "인증/보안 관리 API (관리자 전용)")
@SecurityRequirement(name = "BearerAuth")
public class AuthAdminController {

	private final AuthAdminService authAdminService;

	/**
	 * 로그인 로그 조회 - 필터링 + 시간 범위 + 페이징
	 */
	@GetMapping("/logs/login")
	@Operation(summary = "로그인 로그 조회", description = "최근 n시간 내 로그인 시도 기록")
	public BaseResponse<Page<LoginLogAdminRes>> getLoginLogs(
		@Parameter(description = "이메일 검색") @RequestParam(required = false) String email,
		@Parameter(description = "클라이언트 IP") @RequestParam(required = false) String clientIp,
		@Parameter(description = "성공 여부") @RequestParam(required = false) Boolean success,
		@Parameter(description = "조회 시간 범위(시간)") @RequestParam(defaultValue = "24") int hours,
		@Parameter(hidden = true) @PageableDefault(size = 50, sort = "attemptedAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		Page<LoginLogAdminRes> response = authAdminService.getLoginLogs(email, clientIp, success, hours, pageable);
		return BaseResponse.success(response);
	}

	/**
	 * 회원가입 로그 조회 - 시간 범위 + 페이징
	 */
	@GetMapping("/logs/signup")
	@Operation(summary = "회원가입 로그 조회", description = "최근 n시간 내 가입 기록")
	public BaseResponse<Page<SignupLogAdminRes>> getSignupLogs(
		@Parameter(description = "조회 시간 범위(시간)") @RequestParam(defaultValue = "24") int hours,
		@Parameter(hidden = true) @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		Page<SignupLogAdminRes> response = authAdminService.getSignupLogs(hours, pageable);
		return BaseResponse.success(response);
	}

	/**
	 * 잠긴 계정 목록 조회
	 */
	@GetMapping("/security/locked-accounts")
	@Operation(summary = "잠긴 계정 목록", description = "현재 로그인 잠금 상태인 계정 목록")
	public BaseResponse<List<LockedAccountRes>> getLockedAccounts() {
		return BaseResponse.success(authAdminService.getLockedAccounts());
	}

	/**
	 * 계정 잠금 해제
	 */
	@DeleteMapping("/security/locked-accounts/{email}")
	@Operation(summary = "계정 잠금 해제", description = "특정 회원의 로그인 잠금 해제")
	public BaseResponse<Void> unlockAccount(
		@CurrentUser UserPrincipal admin,
		@Parameter(description = "잠금 해제할 이메일") @PathVariable String email
	) {
		authAdminService.unlockAccount(admin.getId(), email);
		return BaseResponse.success(null);
	}
}
