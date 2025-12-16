package com.back.b2st.domain.member.dto.response;

import com.back.b2st.domain.member.entity.Member;

import lombok.Builder;

@Builder
public record MyInfoRes(
	Long memberId,
	String email,
	String name,
	boolean isVerified
) {
	public static MyInfoRes from(Member member) {
		return MyInfoRes.builder()
			.memberId(member.getId())
			.email(member.getEmail())
			.name(member.getName())
			.isVerified(member.isVerified())
			.build();
	}
}
