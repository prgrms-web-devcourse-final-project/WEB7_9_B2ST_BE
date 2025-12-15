package com.back.b2st.domain.member.dto;

import com.back.b2st.global.common.BankCode;

import jakarta.validation.constraints.NotNull;
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
public class RefundAccountReq {

	@NotNull(message = "은행 코드는 필수입니다.")
	private BankCode bankCode;

	@NotNull(message = "계좌번호는 필수입니다.")
	@Pattern(regexp = "^[0-9]+$", message = "계좌번호는 숫자만 입력해주세요.")
	private String accountNumber;

	@NotNull(message = "예금주명은 필수입니다.")
	private String holderName;
}
