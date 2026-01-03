package com.back.b2st.domain.payment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DomainType {
	RESERVATION("일반 예매"),
	PRERESERVATION("신청 예매"),
	LOTTERY("추첨 예매"),
	TRADE("티켓 거래");

	private final String description;
}
