package com.back.b2st.domain.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.back.b2st.domain.member.dto.PasswordChangeRequest;
import com.back.b2st.domain.member.dto.SignupRequest;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

	@InjectMocks
	private MemberService memberService;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Test
	@DisplayName("회원가입 성공 테스트")
	void signup_success() {
		// given
		SignupRequest request = createSignupRequest("test@email.com", "validPw123!", "tester");

		// Mocking
		given(memberRepository.existsByEmail(request.getEmail())).willReturn(false);
		given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPassword");

		// Mocking: save 호출 시 ID가 1인 Member 반환하도록
		Member savedMember = Member.builder().email(request.getEmail()).role(Member.Role.MEMBER).build();
		ReflectionTestUtils.setField(savedMember, "id", 1L);
		given(memberRepository.save(any(Member.class))).willReturn(savedMember);

		// when
		Long memberId = memberService.signup(request);

		// then
		assertThat(memberId).isEqualTo(1L);
	}

	@Test
	@DisplayName("회원가입 실패 - 이메일 중복")
	void signup_fail_duplicate_email() {
		// given
		SignupRequest request = createSignupRequest("duplicate@email.com", "pw", "name");

		// Mocking
		given(memberRepository.existsByEmail(request.getEmail())).willReturn(true);

		// when & then
		assertThatThrownBy(() -> memberService.signup(request)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("이미 가입된 이메일입니다.");
	}

	// 테스트용 DTO 생성 헬퍼 메서드
	private SignupRequest createSignupRequest(String email, String pw, String name) {
		SignupRequest request = new SignupRequest();
		// Reflection
		ReflectionTestUtils.setField(request, "email", email);
		ReflectionTestUtils.setField(request, "password", pw);
		ReflectionTestUtils.setField(request, "name", name);
		ReflectionTestUtils.setField(request, "birth", LocalDate.of(2000, 1, 1));
		return request;
	}

	@Test
	@DisplayName("비밀번호 변경 성공")
	void changePassword_success() {
		// given
		Long memberId = 1L;
		Member member = Member.builder().password("encodedOldPw").build();

		PasswordChangeRequest request = PasswordChangeRequest.builder()
			.currentPassword("oldPw")
			.newPassword("newPw123!")
			.build();

		given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
		given(passwordEncoder.matches("oldPw", "encodedOldPw")).willReturn(true); // 비번 일치
		given(passwordEncoder.encode("newPw123!")).willReturn("encodedNewPw");

		// when
		memberService.changePassword(memberId, request);

		// then
		assertThat(member.getPassword()).isEqualTo("encodedNewPw");
	}

	@Test
	@DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치")
	void changePassword_fail_mismatch() {
		// given
		Long memberId = 1L;
		Member member = Member.builder().password("encodedOldPw").build();
		PasswordChangeRequest request = PasswordChangeRequest.builder()
			.currentPassword("wrongPw")
			.newPassword("newPw123!")
			.build();

		given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
		given(passwordEncoder.matches("wrongPw", "encodedOldPw")).willReturn(false); // 비번 불일치

		// when & then
		assertThatThrownBy(() -> memberService.changePassword(memberId, request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("현재 비밀번호가 일치하지 않습니다.");
	}
}
