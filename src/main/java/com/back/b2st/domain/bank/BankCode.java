package com.back.b2st.domain.bank;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.back.b2st.domain.bank.error.BankErrorCode;
import com.back.b2st.global.error.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BankCode {

	KB("004", "KB국민은행"),
	SC("023", "SC제일은행"),
	CITY("027", "한국씨티은행"),
	HANA("081", "하나은행"),
	SHINHAN("088", "신한은행"),
	K_BANK("089", "케이뱅크"),
	KAKAO("090", "카카오뱅크"),
	TOSS("092", "토스뱅크"),
	WOORI("020", "우리은행"),
	IBK("003", "IBK기업은행"),
	NONGHYUP("011", "NH농협은행"),
	SAEMAUL("045", "새마을금고"),
	SHINHYUP("048", "신협"),
	POST("071", "우체국");

	private static final Map<String, BankCode> CODE_MAP =
		Arrays.stream(values())
			.collect(Collectors.toUnmodifiableMap(
				BankCode::getCode,
				Function.identity()
			));
	private final String code;
	private final String description;

	@JsonCreator
	public static BankCode fromCode(String code) {
		BankCode bankCode = CODE_MAP.get(code);
		if (bankCode == null) {
			throw new BusinessException(BankErrorCode.INVALID_BANK_CODE, code);
		}
		return bankCode;
	}

	@JsonValue
	public String getCode() {
		return code;
	}
}
