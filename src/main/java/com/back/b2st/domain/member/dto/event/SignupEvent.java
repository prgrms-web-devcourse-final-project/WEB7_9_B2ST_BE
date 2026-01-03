package com.back.b2st.domain.member.dto.event;

import java.time.LocalDateTime;

public record SignupEvent(
	String email,
	String clientIp,
	LocalDateTime occurredAt
) {

	public static SignupEvent of(String email, String clientIp) {
		return new SignupEvent(email, clientIp, LocalDateTime.now());
	}
}
