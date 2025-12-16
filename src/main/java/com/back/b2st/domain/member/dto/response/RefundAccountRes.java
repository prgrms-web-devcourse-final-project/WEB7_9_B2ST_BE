package com.back.b2st.domain.member.dto.response;

import com.back.b2st.domain.member.entity.RefundAccount;

import lombok.Builder;

@Builder
public record RefundAccountRes(
	String bankCode,
	String bankName,
	String accountNumber,
	String holderName
) {

	public static RefundAccountRes from(RefundAccount entity) {
		return RefundAccountRes.builder()
			.bankCode(entity.getBankCode().getCode())
			.bankName(entity.getBankCode().getDescription())
			.accountNumber(entity.getAccountNumber())
			.holderName(entity.getHolderName())
			.build();
	}
}
