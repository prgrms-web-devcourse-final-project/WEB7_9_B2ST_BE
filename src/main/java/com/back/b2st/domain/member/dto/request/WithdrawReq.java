package com.back.b2st.domain.member.dto.request;

import jakarta.validation.constraints.Size;

/**
 * 회원 탈퇴 요청 DTO
 * - 일반 회원(EMAIL): password 필수
 * - 소셜 회원(KAKAO): password 불필요 (null 허용)
 */
public record WithdrawReq(
	@Size(max = 32, message = "비밀번호가 너무 깁니다.") String password) {
}
