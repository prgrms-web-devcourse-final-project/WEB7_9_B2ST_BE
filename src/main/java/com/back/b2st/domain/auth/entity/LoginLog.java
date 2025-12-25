package com.back.b2st.domain.auth.entity;

import java.time.LocalDateTime;
import java.util.Map;

import com.back.b2st.domain.auth.error.AuthErrorCode;
import com.back.b2st.domain.member.error.MemberErrorCode;
import com.back.b2st.global.error.code.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "login_logs", indexes = {
	@Index(name = "idx_login_log_email", columnList = "email"),
	@Index(name = "idx_login_log_client_ip", columnList = "clientIp"),
	@Index(name = "idx_login_log_attempted_at", columnList = "attemptedAt")
})
@SequenceGenerator(name = "login_log_id_gen", sequenceName = "login_logs_seq", allocationSize = 50)
public class LoginLog {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "login_log_id_gen")
	@Column(name = "login_id")
	private Long id;

	@Column(nullable = false, length = 100)
	private String email;

	@Column(nullable = false, length = 45)
	private String clientIp;

	@Column(nullable = false)
	private boolean success;

	@Enumerated(EnumType.STRING)
	@Column(length = 50)
	private FailReason failReason;

	@Column(nullable = false)
	private LocalDateTime attemptedAt;

	@Builder
	public LoginLog(String email, String clientIp, boolean success, FailReason failReason, LocalDateTime attemptedAt) {
		this.email = email;
		this.clientIp = clientIp;
		this.success = success;
		this.failReason = failReason;
		this.attemptedAt = attemptedAt != null ? attemptedAt : LocalDateTime.now();
	}

	public enum FailReason {
		INVALID_PASSWORD,
		ACCOUNT_LOCKED,
		ACCOUNT_WITHDRAWN,
		ACCOUNT_NOT_FOUND;

		private static final Map<ErrorCode, FailReason> errorCodeToFailReason = Map.of(
			AuthErrorCode.ACCOUNT_LOCKED, ACCOUNT_LOCKED,
			MemberErrorCode.ALREADY_WITHDRAWN, ACCOUNT_WITHDRAWN,
			MemberErrorCode.MEMBER_NOT_FOUND, ACCOUNT_NOT_FOUND);

		public static FailReason fromErrorCode(ErrorCode errorCode) {
			if (errorCode == null) {
				return INVALID_PASSWORD;
			}
			return errorCodeToFailReason.getOrDefault(errorCode, INVALID_PASSWORD);
		}
	}
}
