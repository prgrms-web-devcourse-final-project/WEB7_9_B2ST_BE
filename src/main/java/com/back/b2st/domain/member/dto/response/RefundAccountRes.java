package com.back.b2st.domain.member.dto.response;

import com.back.b2st.domain.member.entity.RefundAccount;

public record RefundAccountRes(
	String bankCode,
	String bankName,
	String accountNumber,
	String holderName
) {

	public static RefundAccountRes from(RefundAccount entity) {
		return new RefundAccountRes(
			entity.getBankCode().getCode(),
			entity.getBankCode().getDescription(),
			entity.getAccountNumber(),
			entity.getHolderName()
		);
	}
}
