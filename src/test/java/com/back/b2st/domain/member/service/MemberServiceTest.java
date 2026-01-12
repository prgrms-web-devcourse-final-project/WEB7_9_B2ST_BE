package com.back.b2st.domain.member.service;

import static com.back.b2st.domain.member.fixture.MemberTestFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.back.b2st.domain.auth.repository.RefreshTokenRepository;
import com.back.b2st.domain.member.dto.event.SignupEvent;
import com.back.b2st.domain.member.dto.request.PasswordChangeReq;
import com.back.b2st.domain.member.dto.request.SignupReq;
import com.back.b2st.domain.member.dto.request.WithdrawReq;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.entity.RefundAccount;
import com.back.b2st.domain.member.error.MemberErrorCode;
import com.back.b2st.domain.member.metrics.MemberMetrics;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.member.repository.RefundAccountRepository;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

	// 테스트용 상수
	private static final String TEST_CLIENT_IP = "192.168.1.100";
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
	@Mock
	private SignupRateLimitService signupRateLimitService;
	@Mock
	private ApplicationEventPublisher eventPublisher;
	@Mock
	private MemberMetrics memberMetrics;

	// 헬퍼 메서드
	private SignupReq buildSignupReq() {
		return new SignupReq(
			"test@email.com",
			"validPw123!",
			"tester",
			"01012345678",
			LocalDate.of(2000, 1, 1));
	}

	private PasswordChangeReq buildPasswordChangeReq() {
		return new PasswordChangeReq("oldPw", "newPw123!");
	}

	@Nested
	@DisplayName("회원가입")
	class SignupTest {

		@Test
		@DisplayName("성공")
		void success() {
			// given
			SignupReq request = buildSignupReq();

			willDoNothing().given(signupRateLimitService).checkSignupLimit(TEST_CLIENT_IP);
			given(memberRepository.existsByEmail(request.email())).willReturn(false);
			given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");

			Member savedMember = Member.builder().email(request.email()).role(Member.Role.MEMBER).build();
			ReflectionTestUtils.setField(savedMember, "id", 1L);
			given(memberRepository.save(any(Member.class))).willReturn(savedMember);

			// when
			Long memberId = memberService.signup(request, TEST_CLIENT_IP);

			// then
			assertThat(memberId).isEqualTo(1L);
			then(signupRateLimitService).should().checkSignupLimit(TEST_CLIENT_IP);

			// 가입 이벤트 발행 검증
			ArgumentCaptor<SignupEvent> eventCaptor = ArgumentCaptor.forClass(SignupEvent.class);
			then(eventPublisher).should().publishEvent(eventCaptor.capture());

			SignupEvent publishedEvent = eventCaptor.getValue();
			assertThat(publishedEvent.email()).isEqualTo(request.email());
			assertThat(publishedEvent.clientIp()).isEqualTo(TEST_CLIENT_IP);
		}

		@Test
		@DisplayName("실패 - 이메일 중복")
		void fail_duplicateEmail() {
			// given
			SignupReq request = buildSignupReq();
			willDoNothing().given(signupRateLimitService).checkSignupLimit(TEST_CLIENT_IP);
			given(memberRepository.existsByEmail(request.email())).willReturn(true);

			// when & then
			assertThatThrownBy(() -> memberService.signup(request, TEST_CLIENT_IP))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(MemberErrorCode.DUPLICATE_EMAIL);
		}

		@Test
		@DisplayName("실패 - Rate Limit 초과")
		void fail_rateLimitExceeded() {
			// given
			SignupReq request = buildSignupReq();
			willThrow(new BusinessException(MemberErrorCode.SIGNUP_RATE_LIMIT_EXCEEDED))
				.given(signupRateLimitService).checkSignupLimit(TEST_CLIENT_IP);

			// when & then
			assertThatThrownBy(() -> memberService.signup(request, TEST_CLIENT_IP))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(MemberErrorCode.SIGNUP_RATE_LIMIT_EXCEEDED);

			// memberRepository는 호출되지 않아야 함 (Rate Limit에서 먼저 차단)
			then(memberRepository).shouldHaveNoInteractions();
		}
	}

	@Nested
	@DisplayName("비밀번호 변경")
	class ChangePasswordTest {

		@Test
		@DisplayName("성공")
		void success() {
			Long memberId = 1L;
			Member member = Member.builder().password("encodedOldPw").build();
			PasswordChangeReq request = buildPasswordChangeReq();

			given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
			given(passwordEncoder.matches("oldPw", "encodedOldPw")).willReturn(true);
			given(passwordEncoder.encode("newPw123!")).willReturn("encodedNewPw");

			memberService.changePassword(memberId, request);

			assertThat(member.getPassword()).isEqualTo("encodedNewPw");
		}

		@Test
		@DisplayName("실패 - 현재 비밀번호 불일치")
		void fail_mismatch() {
			Long memberId = 1L;
			Member member = Member.builder().password("encodedOldPw").build();
			PasswordChangeReq request = buildPasswordChangeReq();

			given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
			given(passwordEncoder.matches("oldPw", "encodedOldPw")).willReturn(false);

			assertThatThrownBy(() -> memberService.changePassword(memberId, request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(MemberErrorCode.PASSWORD_MISMATCH);
		}
	}

	@Nested
	@DisplayName("회원 탈퇴")
	class WithdrawTest {

		@Test
		@DisplayName("성공")
		void success() {
			Member member = createMemberWithId(1L, "test@test.com", "encodedPw");
			WithdrawReq request = buildWithdrawReq("rawPassword");

			given(memberRepository.findById(1L)).willReturn(Optional.of(member));
			given(passwordEncoder.matches("rawPassword", "encodedPw")).willReturn(true);
			given(refundAccountRepository.findByMember(member)).willReturn(Optional.empty());

			memberService.withdraw(1L, request);

			assertThat(member.isDeleted()).isTrue();
			then(refreshTokenRepository).should().deleteById("test@test.com");
		}

		@Test
		@DisplayName("실패 - 비밀번호 불일치")
		void fail_passwordMismatch() {
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
		@DisplayName("실패 - 이미 탈퇴한 회원")
		void fail_alreadyWithdrawn() {
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
		void deletesRefundAccount() {
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
}
