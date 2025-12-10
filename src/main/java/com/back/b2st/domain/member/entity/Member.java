package com.back.b2st.domain.member.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.back.b2st.global.jpa.entity.BaseEntity;

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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "members", indexes = {
	@Index(name = "idx_member_provider_id", columnList = "provider, provider_id")
})
public class Member extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_id_gen")
	@SequenceGenerator(
		name = "member_id_gen",
		sequenceName = "members_seq",
		allocationSize = 50
	)
	@Column(name = "member_id")
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = true) // 소셜 로그인은 비밀번호 없을 수 있음
	private String password;

	@Column(nullable = false)
	private String name;

	private String phone;

	private LocalDate birth;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role; // MEMBER, ADMIN

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Provider provider; // EMAIL, KAKAO

	@Column(name = "provider_id")
	private String providerId;

	@Column(name = "is_verified")
	private boolean isVerified; // 본인인증 여부

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Builder
	public Member(String email, String password, String name, String nickname, String phone, LocalDate birth, Role role,
		Provider provider, String providerId, boolean isVerified) {
		this.email = email;
		this.password = password;
		this.name = name;
		this.phone = phone;
		this.birth = birth;
		this.role = role;
		this.provider = provider;
		this.providerId = providerId;
		this.isVerified = isVerified;
	}

	public enum Role {
		MEMBER, ADMIN
	}

	public enum Provider {
		EMAIL, KAKAO
	}
}