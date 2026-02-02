package com.back.b2st.domain.member.dto.response;

import java.time.LocalDateTime;

import com.back.b2st.domain.member.entity.Member;

public record MemberSummaryAdminRes(
	Long memberId,
	String email,
	String name,
	Member.Role role,
	Member.Provider provider,
	boolean isEmailVerified,
	boolean isIdentityVerified,
	boolean isDeleted,
	LocalDateTime createdAt,
	LocalDateTime deletedAt
) {
	public static MemberSummaryAdminRes from(Member member) {
		return new MemberSummaryAdminRes(
			member.getId(),
			member.getEmail(),
			member.getName(),
			member.getRole(),
			member.getProvider(),
			member.isEmailVerified(),
			member.isIdentityVerified(),
			member.isDeleted(),
			member.getCreatedAt(),
			member.getDeletedAt()
		);
	}
}
