package com.back.b2st.domain.member.dto.response;

import com.back.b2st.domain.member.entity.Member;

public record MyInfoRes(
	Long memberId,
	String email,
	String name,
	boolean isEmailVerified,
	boolean isIdentityVerified
) {
	public static MyInfoRes from(Member member) {
		return new MyInfoRes(
			member.getId(),
			member.getEmail(),
			member.getName(),
			member.isEmailVerified(),
			member.isIdentityVerified()
		);
	}
}
