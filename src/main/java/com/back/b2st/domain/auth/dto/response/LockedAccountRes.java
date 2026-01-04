package com.back.b2st.domain.auth.dto.response;

public record LockedAccountRes(
	String email,
	long remainingSeconds,
	int remainingMinutes
) {
	public static LockedAccountRes of(String email, long remainingSeconds) {
		return new LockedAccountRes(
			email,
			remainingSeconds,
			(int)Math.ceil(remainingSeconds / 60.0));
	}
}
