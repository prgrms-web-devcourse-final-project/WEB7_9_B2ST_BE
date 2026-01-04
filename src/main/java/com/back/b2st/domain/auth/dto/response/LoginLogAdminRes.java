package com.back.b2st.domain.auth.dto.response;

import java.time.LocalDateTime;

import com.back.b2st.domain.auth.entity.LoginLog;

public record LoginLogAdminRes(
	Long id,
	String email,
	String clientIp,
	boolean success,
	LoginLog.FailReason failReason,
	LocalDateTime attemptedAt
) {
	public static LoginLogAdminRes from(LoginLog log) {
		return new LoginLogAdminRes(
			log.getId(),
			log.getEmail(),
			log.getClientIp(),
			log.isSuccess(),
			log.getFailReason(),
			log.getAttemptedAt()
		);
	}
}
