package com.back.b2st.domain.member.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.member.dto.request.SignupReq;
import com.back.b2st.domain.member.service.MemberService;
import com.back.b2st.global.common.BaseResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;

	/**
	 * 회원가입 처리 - Bean Validation(정규표현식) + BCrypt 암호화 + 이메일 중복 검사 + 기본 Role 설정
	 *
	 * @param request 회원가입 요청 정보
	 * @return 생성된 회원 ID
	 */
	@PostMapping("/signup")
	public BaseResponse<Long> signup(@Valid @RequestBody SignupReq request) {
		Long memberId = memberService.signup(request);
		return BaseResponse.created(memberId);
	}
}
