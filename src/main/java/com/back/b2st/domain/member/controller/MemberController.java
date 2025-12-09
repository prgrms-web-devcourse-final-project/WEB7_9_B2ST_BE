package com.back.b2st.domain.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.member.dto.SignupRequest;
import com.back.b2st.domain.member.service.MemberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;

	@PostMapping("/signup")
	public ResponseEntity<String> signup(@Valid @RequestBody SignupRequest request) {
		Long memberId = memberService.signup(request);
		return ResponseEntity.ok("(response 수정 필요)회원가입 성공 ID: " + memberId);
	}
}
