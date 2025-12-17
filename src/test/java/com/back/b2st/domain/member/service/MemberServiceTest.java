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

import com.back.b2st.domain.member.dto.request.PasswordChangeReq;
import com.back.b2st.domain.member.dto.request.SignupReq;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.error.MemberErrorCode;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.global.error.exception.BusinessException;

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

		SignupReq request = buildSignupReq();

		// Mocking
		given(memberRepository.existsByEmail(request.email())).willReturn(false);
		given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");

		// Mocking: save 호출 시 ID가 1인 Member 반환하도록
		Member savedMember = Member.builder().email(request.email()).role(Member.Role.MEMBER).build();
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
		SignupReq request = buildSignupReq();

		// Mocking
		given(memberRepository.existsByEmail(request.email())).willReturn(true);

		// when & then
		assertThatThrownBy(() -> memberService.signup(request)).isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(MemberErrorCode.DUPLICATE_EMAIL);
	}

	@Test
	@DisplayName("비밀번호 변경 성공")
	void changePassword_success() {
		// given
		Long memberId = 1L;
		Member member = Member.builder().password("encodedOldPw").build();

		PasswordChangeReq request = buildPasswordChangeReq();

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
		PasswordChangeReq request = buildPasswordChangeReq();

		given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
		given(passwordEncoder.matches("oldPw", "encodedOldPw")).willReturn(false); // 비번 불일치

		// when & then
		assertThatThrownBy(() -> memberService.changePassword(memberId, request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(MemberErrorCode.PASSWORD_MISMATCH);
	}

	private SignupReq buildSignupReq() {
		return new SignupReq(
			"test@email.com",
			"validPw123!",
			"tester",
			"01012345678",
			LocalDate.of(2000, 1, 1)
		);
	}

	private PasswordChangeReq buildPasswordChangeReq() {
		return new PasswordChangeReq(
			"oldPw",
			"newPw123!"
		);
	}
}
