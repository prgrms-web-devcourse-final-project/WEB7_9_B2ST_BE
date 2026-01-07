package com.back.b2st.domain.member.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupReq(
	@NotBlank(message = "이메일은 필수입니다.")
	@Email(regexp = "^[a-zA-Z0-9+-\\_.]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$",
		message = "올바른 이메일 형식이 아닙니다.")
	@Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다.")
	String email,

	@NotBlank(message = "비밀번호는 필수입니다.")
	@Pattern(regexp = "^(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,30}$",
		message = "비밀번호는 8~30자, 영소문자+숫자+특수기호(@$!%*?&)를 포함해야 합니다.")
	String password,

	@NotBlank(message = "이름은 필수입니다.")
	@Size(min = 2, max = 20, message = "이름은 2자 이상 20자 이하로 입력해주세요.")
	@Pattern(regexp = "^[가-힣a-zA-Z]+$", message = "이름은 한글 또는 영문만 입력 가능합니다.")
	String name,

	@NotBlank(message = "전화번호는 필수입니다.")
	@Pattern(regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$",
		message = "전화번호는 하이픈(-) 없이 10~11자리 숫자만 입력 가능합니다.")
	String phone,

	@NotNull(message = "생년월일은 필수입니다.")
	@Past(message = "생년월일은 과거 날짜여야 합니다.")
	LocalDate birth
) {
}
