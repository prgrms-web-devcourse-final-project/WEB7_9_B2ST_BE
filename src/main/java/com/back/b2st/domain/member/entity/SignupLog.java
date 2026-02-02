package com.back.b2st.domain.member.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * 회원 가입 시도 로그
 * - 가입 시도/성공 기록
 * - 보안 분석 및 이상 징후 탐지용
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "signup_logs", indexes = {
	@Index(name = "idx_signup_log_email", columnList = "email"),
	@Index(name = "idx_signup_log_client_ip", columnList = "clientIp"),
	@Index(name = "idx_signup_log_created_at", columnList = "createdAt"),
	@Index(name = "idx_signup_log_ip_time", columnList = "clientIp, createdAt")
})
@SequenceGenerator(
	name = "signup_log_id_gen",
	sequenceName = "signup_logs_seq",
	allocationSize = 50
)
public class SignupLog {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "signup_log_id_gen")
	@Column(name = "signup_log_id")
	private Long id;

	@Column(nullable = false, length = 100)
	private String email;

	@Column(nullable = false, length = 45)
	private String clientIp;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Builder
	public SignupLog(String email, String clientIp, LocalDateTime createdAt) {
		this.email = email;
		this.clientIp = clientIp;
		this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
	}
}
