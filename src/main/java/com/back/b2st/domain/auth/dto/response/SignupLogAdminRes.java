package com.back.b2st.domain.auth.dto.response;

import java.time.LocalDateTime;

import com.back.b2st.domain.member.entity.SignupLog;

public record SignupLogAdminRes(
	Long id,
	String email,
	String clientIp,
	LocalDateTime createdAt
) {
	public static SignupLogAdminRes from(SignupLog log) {
		return new SignupLogAdminRes(
			log.getId(),
			log.getEmail(),
			log.getClientIp(),
			log.getCreatedAt()
		);
	}
}
