package com.back.b2st.domain.email.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyCodeReq(
	@NotBlank(message = "이메일은 필수입니다.")
	@Email(regexp = "^[a-zA-Z0-9+-\\_.]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$",
		message = "올바른 이메일 형식이 아닙니다.")
	@Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다.")
	String email,

	@NotBlank(message = "인증 코드는 필수입니다.")
	@Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 6자리 숫자입니다.")
	String code
) {
}
