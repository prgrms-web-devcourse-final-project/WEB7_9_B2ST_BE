package com.back.b2st.domain.bank.dto.response;

import com.back.b2st.domain.bank.BankCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BankRes {

	private String code; // 서버
	private String name; // 화면 표시

	public static BankRes from(BankCode bankCode) {
		return new BankRes(bankCode.getCode(), bankCode.getDescription());
	}
}
