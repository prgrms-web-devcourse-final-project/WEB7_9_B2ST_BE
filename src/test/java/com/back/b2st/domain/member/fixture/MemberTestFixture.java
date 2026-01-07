package com.back.b2st.domain.member.fixture;

import java.time.LocalDate;

import org.springframework.test.util.ReflectionTestUtils;

import com.back.b2st.domain.member.dto.request.WithdrawReq;
import com.back.b2st.domain.member.entity.Member;

public class MemberTestFixture {

	public static Member createMember(String email, String encodedPassword, String name) {
		return Member.builder()
			.email(email)
			.password(encodedPassword)
			.name(name)
			.phone("01012345678")
			.birth(LocalDate.of(1990, 1, 1))
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(false)
			.build();
	}

	public static Member createMemberWithId(Long id, String email, String encodedPassword) {
		Member member = createMember(email, encodedPassword, "테스트유저");
		ReflectionTestUtils.setField(member, "id", id);
		return member;
	}

	public static Member createAdminMemberWithId(Long id, String email, String encodedPassword) {
		Member member = Member.builder()
			.email(email)
			.password(encodedPassword)
			.name("관리자")
			.phone("01012345678")
			.birth(LocalDate.of(1990, 1, 1))
			.role(Member.Role.ADMIN)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(false)
			.build();
		ReflectionTestUtils.setField(member, "id", id);
		return member;
	}

	// 밑으로 dto 빌더 헬퍼
	public static WithdrawReq buildWithdrawReq(String password) {
		return new WithdrawReq(password);
	}
}
