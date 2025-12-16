package com.back.b2st.domain.member.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignupReq(
	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	String email,

	@NotBlank(message = "비밀번호는 필수입니다.")
	// 비밀번호는 8~30자, 영문자+숫자+특수기호 포함
	@Pattern(regexp = "^(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,30}$",
		message = "비밀번호는 8~30자, 영소문자+숫자+특수기호를 포함해야 합니다.")
	String password,

	@NotBlank(message = "이름은 필수입니다.")
	String name,

	String phone,

	LocalDate birth
) {
}
