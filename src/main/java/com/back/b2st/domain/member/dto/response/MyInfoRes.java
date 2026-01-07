package com.back.b2st.domain.member.dto.response;

import java.time.LocalDate;

import com.back.b2st.domain.member.entity.Member;

public record MyInfoRes(
	Long memberId,
	String email,
	String name,
	String phone,
	LocalDate birth,
	boolean isEmailVerified,
	boolean isIdentityVerified) {
	public static MyInfoRes from(Member member) {
		return new MyInfoRes(
			member.getId(),
			member.getEmail(),
			member.getName(),
			member.getPhone(),
			member.getBirth(),
			member.isEmailVerified(),
			member.isIdentityVerified());
	}
}
