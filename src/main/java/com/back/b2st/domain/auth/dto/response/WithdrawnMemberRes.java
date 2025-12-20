package com.back.b2st.domain.auth.dto.response;

import static com.back.b2st.global.util.MaskingUtil.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record WithdrawnMemberRes(
	boolean isWithdrawn,
	int remainingDays,
	String maskedEmail
) {

	public static WithdrawnMemberRes from(String email, LocalDateTime deletedAt) {
		LocalDateTime expiryDate = LocalDateTime.now().plusDays(30);
		int remaing = (int)ChronoUnit.DAYS.between(LocalDateTime.now(), expiryDate);

		String masked = maskEmail(email);

		return new WithdrawnMemberRes(true, Math.max(0, remaing), masked);
	}
}
