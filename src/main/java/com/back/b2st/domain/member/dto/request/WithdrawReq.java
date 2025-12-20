package com.back.b2st.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WithdrawReq(
	@NotBlank(message = "비밀번호를 입력해주세요.")
	@Size(max = 32, message = "비밀번호가 너무 깁니다.")
	String password
) {
}
