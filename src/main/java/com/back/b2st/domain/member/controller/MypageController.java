package com.back.b2st.domain.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.member.dto.MyInfoResponse;
import com.back.b2st.domain.member.service.MemberService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MypageController {

	private final MemberService memberService;

	@GetMapping("/me")
	public ResponseEntity<BaseResponse<MyInfoResponse>> getMyInfo(@CurrentUser UserPrincipal userPrincipal) {
		MyInfoResponse myInfo = memberService.getMyInfo(userPrincipal.getId());

		return ResponseEntity.ok(BaseResponse.success(myInfo));
	}
}
