package com.back.b2st.domain.auth.dto.response;

import static com.back.b2st.global.util.MaskingUtil.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record WithdrawnMemberRes(
	boolean isWithdrawn,
	int remainingDays,
	String maskedEmail) {

	public static WithdrawnMemberRes from(String email, LocalDateTime deletedAt) {
		// deletedAt 기준으로 30일 후 만료
		LocalDateTime expiryDate = deletedAt.plusDays(30);
		int remaining = (int)ChronoUnit.DAYS.between(LocalDateTime.now(), expiryDate);

		String masked = maskEmail(email);

		return new WithdrawnMemberRes(true, Math.max(0, remaining), masked);
	}
}
