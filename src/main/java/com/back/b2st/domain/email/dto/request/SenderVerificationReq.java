package com.back.b2st.domain.email.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SenderVerificationReq(
	@NotBlank(message = "이메일은 필수입니다.")
	@Email(regexp = "^[a-zA-Z0-9+-\\_.]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$",
		message = "올바른 이메일 형식이 아닙니다.")
	@Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다.")
	String email
) {
}
