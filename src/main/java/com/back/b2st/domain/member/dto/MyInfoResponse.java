package com.back.b2st.domain.member.dto;

import com.back.b2st.domain.member.entity.Member;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyInfoResponse {
	private Long memberId;
	private String email;
	private String name;
	private boolean isVerified;

	// todo: 환불 계좌 넣어놔야 함(아마 RefundAccount dto. dto네임은 dto대신 뭐로할지 생각 좀 해야할 것 같음)

	public static MyInfoResponse from(Member member) {
		return MyInfoResponse.builder()
			.memberId(member.getId())
			.email(member.getEmail())
			.name(member.getName())
			.isVerified(member.isVerified())
			.build();
	}

}
