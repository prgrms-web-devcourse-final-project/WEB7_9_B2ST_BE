package com.back.b2st.domain.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.member.dto.request.PasswordChangeReq;
import com.back.b2st.domain.member.dto.request.RefundAccountReq;
import com.back.b2st.domain.member.dto.response.MyInfoRes;
import com.back.b2st.domain.member.dto.response.RefundAccountRes;
import com.back.b2st.domain.member.service.MemberService;
import com.back.b2st.domain.member.service.RefundAccountService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage")
public class MypageController {

	private final MemberService memberService;
	private final RefundAccountService refundAccountService;

	@GetMapping("/me")
	public ResponseEntity<BaseResponse<MyInfoRes>> getMyInfo(@CurrentUser UserPrincipal userPrincipal) {
		MyInfoRes myInfo = memberService.getMyInfo(userPrincipal.getId());

		return ResponseEntity.ok(BaseResponse.success(myInfo));
	}

	@PatchMapping("/password")
	public BaseResponse<Void> changePassword(@CurrentUser UserPrincipal userPrincipal,
		@Valid @RequestBody PasswordChangeReq request) {
		memberService.changePassword(userPrincipal.getId(), request);
		return BaseResponse.success(null);
	}

	@PostMapping("/account")
	public BaseResponse<Void> setRefunAccount(
		@CurrentUser UserPrincipal userPrincipal,
		@Valid @RequestBody RefundAccountReq refundAccountReq) {
		refundAccountService.saveAccount(userPrincipal.getId(), refundAccountReq);
		return BaseResponse.success(null);
	}

	@GetMapping("/account")
	public BaseResponse<RefundAccountRes> getRefundAccount(@CurrentUser UserPrincipal userPrincipal) {
		RefundAccountRes response = refundAccountService.getAccount(userPrincipal.getId());
		return BaseResponse.success(response);
	}
}
