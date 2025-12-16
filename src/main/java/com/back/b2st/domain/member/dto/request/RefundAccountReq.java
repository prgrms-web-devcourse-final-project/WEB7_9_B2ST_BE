package com.back.b2st.domain.member.dto.request;

import com.back.b2st.domain.bank.BankCode;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RefundAccountReq(

	@NotNull(message = "은행 코드는 필수입니다.")
	BankCode bankCode,

	@NotNull(message = "계좌번호는 필수입니다.")
	@Pattern(regexp = "^[0-9]+$", message = "계좌번호는 숫자만 입력해주세요.")
	String accountNumber,

	@NotNull(message = "예금주명은 필수입니다.")
	String holderName
) {
}
