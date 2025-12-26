package com.back.b2st.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

	@InjectMocks
	private CustomUserDetailsService userDetailsService;

	@Mock
	private MemberRepository memberRepository;

	@Nested
	@DisplayName("loadUserByUsername")
	class LoadUserByUsernameTest {

		@Test
		@DisplayName("성공 - 정상 회원 조회")
		void success() {
			// given
			String email = "test@test.com";
			Member member = createActiveMember(email);
			given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));

			// when
			UserDetails userDetails = userDetailsService.loadUserByUsername(email);

			// then
			assertThat(userDetails).isNotNull();
			assertThat(userDetails.getUsername()).isEqualTo(email);
			assertThat(userDetails).isInstanceOf(CustomUserDetails.class);
		}

		@Test
		@DisplayName("실패 - 회원 없음")
		void fail_memberNotFound() {
			// given
			String email = "notfound@test.com";
			given(memberRepository.findByEmail(email)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessageContaining("해당하는 회원을 찾을 수 없습니다");
		}

		@Test
		@DisplayName("실패 - 탈퇴 회원")
		void fail_withdrawnMember() {
			// given
			String email = "withdrawn@test.com";
			Member member = createWithdrawnMember(email);
			given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));

			// when & then
			assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessageContaining("탈퇴한 회원입니다");
		}
	}

	// 헬퍼 메서드

	private Member createActiveMember(String email) {
		return Member.builder()
			.email(email)
			.password("encodedPassword")
			.name("테스트유저")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.isIdentityVerified(true)
			.build();
	}

	private Member createWithdrawnMember(String email) {
		Member member = createActiveMember(email);
		member.softDelete();
		return member;
	}
}
