package com.back.b2st.domain.payment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
	CARD("카드 결제", false),
	EASY_PAY("간편 결제", false),
	VIRTUAL_ACCOUNT("무통장 입금", true);

	private final String description;
	private final boolean requiresDeposit;
}
