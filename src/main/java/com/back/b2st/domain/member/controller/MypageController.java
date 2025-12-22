package com.back.b2st.domain.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.member.dto.request.PasswordChangeReq;
import com.back.b2st.domain.member.dto.request.RefundAccountReq;
import com.back.b2st.domain.member.dto.request.WithdrawReq;
import com.back.b2st.domain.member.dto.response.MyInfoRes;
import com.back.b2st.domain.member.dto.response.RefundAccountRes;
import com.back.b2st.domain.member.service.MemberService;
import com.back.b2st.domain.member.service.RefundAccountService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage")
public class MypageController {

	private final MemberService memberService;
	private final RefundAccountService refundAccountService;

	// @CurrentUser(SpEL 기반 커스텀 어노테이션) + DTO 변환(from 팩토리)
	@GetMapping("/me")
	public ResponseEntity<BaseResponse<MyInfoRes>> getMyInfo(@CurrentUser UserPrincipal userPrincipal) {
		MyInfoRes myInfo = memberService.getMyInfo(userPrincipal.getId());

		return ResponseEntity.ok(BaseResponse.success(myInfo));
	}

	// 현재 비밀번호 검증 + 동일 비밀번호 방지 + BCrypt 재암호화
	@PatchMapping("/password")
	public BaseResponse<Void> changePassword(@CurrentUser UserPrincipal userPrincipal,
			@Valid @RequestBody PasswordChangeReq request) {
		memberService.changePassword(userPrincipal.getId(), request);
		return BaseResponse.success(null);
	}

	// Soft Delete + Redis 토큰 삭제 + 환불 계좌 삭제 + 30일 복구 유예
	@DeleteMapping("/withdraw")
	@Operation(summary = "회원 탈퇴", description = "비밀번호 확인 후 탈퇴 처리 (30일간 복구 가능)")
	public BaseResponse<Void> withdraw(@CurrentUser UserPrincipal userPrincipal,
			@Valid @RequestBody WithdrawReq request) {
		memberService.withdraw(userPrincipal.getId(), request);
		return BaseResponse.success(null);
	}

	// Upsert 패턴(없으면 생성, 있으면 수정) + 계좌번호 마스킹 로그
	@PostMapping("/account")
	public BaseResponse<Void> setRefunAccount(
			@CurrentUser UserPrincipal userPrincipal,
			@Valid @RequestBody RefundAccountReq refundAccountReq) {
		refundAccountService.saveAccount(userPrincipal.getId(), refundAccountReq);
		return BaseResponse.success(null);
	}

	// 계좌번호 마스킹(앞3 + *** + 뒤3) + BankCode Enum 매핑
	@GetMapping("/account")
	public BaseResponse<RefundAccountRes> getRefundAccount(@CurrentUser UserPrincipal userPrincipal) {
		RefundAccountRes response = refundAccountService.getAccount(userPrincipal.getId());
		return BaseResponse.success(response);
	}
}
