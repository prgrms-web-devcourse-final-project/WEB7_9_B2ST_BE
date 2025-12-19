package com.back.b2st.domain.member.service;

import static com.back.b2st.domain.member.fixture.MemberTestFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.back.b2st.domain.auth.repository.RefreshTokenRepository;
import com.back.b2st.domain.member.dto.request.PasswordChangeReq;
import com.back.b2st.domain.member.dto.request.SignupReq;
import com.back.b2st.domain.member.dto.request.WithdrawReq;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.entity.RefundAccount;
import com.back.b2st.domain.member.error.MemberErrorCode;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.member.repository.RefundAccountRepository;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

	@InjectMocks
	private MemberService memberService;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private RefundAccountRepository refundAccountRepository;

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

	@Nested
	@DisplayName("회원 탈퇴 테스트")
	class WithdrawTest {

		@Test
		@DisplayName("정상 탈퇴 성공")
		void withdraw_success() {
			// given
			Member member = createMemberWithId(1L, "test@test.com", "encodedPw");
			WithdrawReq request = buildWithdrawReq("rawPassword");

			given(memberRepository.findById(1L)).willReturn(Optional.of(member));
			given(passwordEncoder.matches("rawPassword", "encodedPw")).willReturn(true);
			given(refundAccountRepository.findByMember(member)).willReturn(Optional.empty());

			// when
			memberService.withdraw(1L, request);

			// then
			assertThat(member.isDeleted()).isTrue();
			then(refreshTokenRepository).should().deleteById("test@test.com");
		}

		@Test
		@DisplayName("탈퇴 실패 - 비밀번호 불일치")
		void withdraw_fail_passwordMismatch() {
			Member member = createMemberWithId(1L, "test@test.com", "encodedPw");
			WithdrawReq request = buildWithdrawReq("wrongPassword");

			given(memberRepository.findById(1L)).willReturn(Optional.of(member));
			given(passwordEncoder.matches("wrongPassword", "encodedPw")).willReturn(false);

			assertThatThrownBy(() -> memberService.withdraw(1L, request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(MemberErrorCode.PASSWORD_MISMATCH);
		}

		@Test
		@DisplayName("탈퇴 실패 - 이미 탈퇴한 회원")
		void withdraw_fail_alreadyWithdrawn() {
			Member member = createMemberWithId(1L, "test@test.com", "encodedPw");
			member.softDelete();
			WithdrawReq request = buildWithdrawReq("rawPassword");

			given(memberRepository.findById(1L)).willReturn(Optional.of(member));

			assertThatThrownBy(() -> memberService.withdraw(1L, request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(MemberErrorCode.ALREADY_WITHDRAWN);
		}

		@Test
		@DisplayName("탈퇴 시 환불 계좌도 함께 삭제")
		void withdraw_deletesRefundAccount() {
			Member member = createMemberWithId(1L, "test@test.com", "encodedPw");
			WithdrawReq request = buildWithdrawReq("rawPassword");
			RefundAccount account = mock(RefundAccount.class);

			given(memberRepository.findById(1L)).willReturn(Optional.of(member));
			given(passwordEncoder.matches("rawPassword", "encodedPw")).willReturn(true);
			given(refundAccountRepository.findByMember(member)).willReturn(Optional.of(account));

			memberService.withdraw(1L, request);

			then(refundAccountRepository).should().delete(account);
		}
	}

	@Nested
	@DisplayName("탈퇴 철회 테스트")
	class CancelWithdrawalTest {

		@Test
		@DisplayName("30일 이내 탈퇴 철회 성공")
		void cancelWithdrawal_success() {
			Member member = createMemberWithId(1L, "test@test.com", "encodedPw");
			member.softDelete();
			given(memberRepository.findById(1L)).willReturn(Optional.of(member));

			memberService.cancelWithdrawal(1L);

			assertThat(member.isDeleted()).isFalse();
		}

		@Test
		@DisplayName("탈퇴 철회 실패 - 탈퇴 상태 아님")
		void cancelWithdrawal_fail_notWithdrawn() {
			Member member = createMemberWithId(1L, "test@test.com", "encodedPw");
			given(memberRepository.findById(1L)).willReturn(Optional.of(member));

			assertThatThrownBy(() -> memberService.cancelWithdrawal(1L))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(MemberErrorCode.NOT_WITHDRAWN);
		}

		@Test
		@DisplayName("탈퇴 철회 실패 - 30일 초과")
		void cancelWithdrawal_fail_expired() {
			Member member = createMemberWithId(1L, "test@test.com", "encodedPw");
			member.softDelete();
			ReflectionTestUtils.setField(member, "deletedAt", LocalDateTime.now().minusDays(35));
			given(memberRepository.findById(1L)).willReturn(Optional.of(member));

			assertThatThrownBy(() -> memberService.cancelWithdrawal(1L))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(MemberErrorCode.WITHDRAWAL_PERIOD_EXPIRED);
		}
	}

	// 밑으로 헬퍼
	private SignupReq buildSignupReq() {
		return new SignupReq(
			"test@email.com",
			"validPw123!",
			"tester",
			"01012345678",
			LocalDate.of(2000, 1, 1));
	}

	private PasswordChangeReq buildPasswordChangeReq() {
		return new PasswordChangeReq(
			"oldPw",
			"newPw123!");
	}
}
