package com.back.b2st.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordChangeReq {

	@NotBlank(message = "현재 비밀번호를 입력해주세요.")
	private String currentPassword;

	@NotBlank(message = "새 비밀번호를 입력해주세요.")
	@Pattern(regexp = "^(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,30}$",
		message = "비밀번호는 8~30자, 영소문자+숫자+특수기호를 포함해야 합니다.")
	private String newPassword;
}
