package com.back.b2st.domain.member.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.back.b2st.domain.member.entity.Member;

public record MemberDetailAdminRes(
	Long id,
	String email,
	String name,
	String phone,
	LocalDate birth,
	Member.Role role,
	Member.Provider provider,
	String providerId,
	boolean isEmailVerified,
	boolean isIdentityVerified,
	boolean isDeleted,
	LocalDateTime createdAt,
	LocalDateTime modifiedAt,
	LocalDateTime deletedAt
) {
	public static MemberDetailAdminRes from(Member member) {
		return new MemberDetailAdminRes(
			member.getId(),
			member.getEmail(),
			member.getName(),
			member.getPhone(),
			member.getBirth(),
			member.getRole(),
			member.getProvider(),
			member.getProviderId(),
			member.isEmailVerified(),
			member.isIdentityVerified(),
			member.isDeleted(),
			member.getCreatedAt(),
			member.getModifiedAt(),
			member.getDeletedAt()
		);
	}
}
