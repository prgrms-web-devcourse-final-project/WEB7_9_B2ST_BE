package com.back.b2st.domain.member.dto.request;

import com.back.b2st.domain.bank.BankCode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RefundAccountReq(

	@NotNull(message = "은행 코드는 필수입니다.")
	BankCode bankCode,

	@NotBlank(message = "계좌번호는 필수입니다.")
	@Pattern(regexp = "^[0-9]{7,16}$", message = "계좌번호는 7~16자리 숫자입니다.")
	String accountNumber,

	@NotBlank(message = "예금주명은 필수입니다.")
	@Size(min = 2, max = 20, message = "예금주명은 2~20자여야 합니다.")
	@Pattern(regexp = "^[가-힣a-zA-Z]+$", message = "예금주명은 한글 또는 영문만 가능합니다.")
	String holderName
) {
}
