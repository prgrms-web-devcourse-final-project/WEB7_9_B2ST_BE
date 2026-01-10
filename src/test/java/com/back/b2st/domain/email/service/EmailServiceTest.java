package com.back.b2st.domain.email.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.email.dto.request.CheckDuplicateReq;
import com.back.b2st.domain.email.dto.request.SenderVerificationReq;
import com.back.b2st.domain.email.dto.request.VerifyCodeReq;
import com.back.b2st.domain.email.dto.response.CheckDuplicateRes;
import com.back.b2st.domain.email.entity.EmailVerification;
import com.back.b2st.domain.email.error.EmailErrorCode;
import com.back.b2st.domain.email.metrics.EmailMetrics;
import com.back.b2st.domain.email.repository.EmailVerificationRepository;
import com.back.b2st.domain.lottery.result.dto.LotteryResultEmailInfo;
import com.back.b2st.domain.lottery.result.repository.LotteryResultRepository;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.domain.seat.grade.entity.SeatGradeType;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
	@InjectMocks
	private EmailService emailService;
	@Mock
	private EmailVerificationRepository emailVerificationRepository;
	@Mock
	private MemberRepository memberRepository;
	@Mock
	private EmailRateLimiter rateLimiter;
	@Mock
	private LotteryResultRepository lotteryResultRepository;
	@Mock
	private EmailSender emailSender;
	@Mock
	private EmailMetrics emailMetrics;

	@Test
	@DisplayName("중복 확인 - 사용 가능한 이메일")
	void checkDuplicate_available() {
		// given
		String email = "new@test.com";
		given(memberRepository.existsByEmail(email)).willReturn(false);
		// when
		CheckDuplicateRes result = emailService.checkDuplicate(new CheckDuplicateReq(email));
		// then
		assertThat(result.available()).isTrue();
	}

	@Test
	@DisplayName("인증 코드 발송 실패 - 이미 인증된 회원")
	void sendVerificationCode_alreadyVerified() {
		// given
		String email = "verified@test.com";
		Member member = Member.builder()
			.email(email)
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(true)
			.build();
		given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));

		// when & then
		assertThatThrownBy(() -> emailService.sendVerificationCode(new SenderVerificationReq(email))).isInstanceOf(
			BusinessException.class).extracting("errorCode").isEqualTo(EmailErrorCode.ALREADY_VERIFIED);
	}

	@Test
	@DisplayName("중복 확인 - 이미 가입된 이메일")
	void checkDuplicate_alreadyExists() {
		// given
		String email = "exists@test.com";
		given(memberRepository.existsByEmail(email)).willReturn(true);
		// when
		CheckDuplicateRes result = emailService.checkDuplicate(new CheckDuplicateReq(email));
		// then
		assertThat(result.available()).isFalse();
	}

	@Test
	@DisplayName("인증 성공 - 코드 일치")
	void verifyCode_success() {
		// given
		String email = "test@test.com";
		String code = "123456";
		EmailVerification verification = EmailVerification.builder().email(email).code(code).attemptCount(0).build();
		Member member = Member.builder()
			.email(email)
			.name("테스터")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isEmailVerified(false)
			.isIdentityVerified(false)
			.build();
		given(emailVerificationRepository.findById(email)).willReturn(Optional.of(verification));
		given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
		// when
		emailService.verifyCode(new VerifyCodeReq(email, code));
		// then
		assertThat(member.isEmailVerified()).isTrue();
		verify(emailVerificationRepository).deleteById(email);
	}

	@Test
	@DisplayName("인증 실패 - 코드 불일치")
	void verifyCode_wrongCode() {
		// given
		String email = "test@test.com";
		EmailVerification verification = EmailVerification.builder()
			.email(email)
			.code("123456")
			.attemptCount(0)
			.build();
		given(emailVerificationRepository.findById(email)).willReturn(Optional.of(verification));
		// when & then
		assertThatThrownBy(() -> emailService.verifyCode(new VerifyCodeReq(email, "000000"))).isInstanceOf(
			BusinessException.class).extracting("errorCode").isEqualTo(EmailErrorCode.VERIFICATION_CODE_MISMATCH);
		verify(emailVerificationRepository).save(any(EmailVerification.class));
	}

	@Test
	@DisplayName("인증 실패 - 시도 횟수 초과")
	void verifyCode_maxAttempt() {
		// given
		String email = "test@test.com";
		EmailVerification verification = EmailVerification.builder()
			.email(email)
			.code("123456")
			.attemptCount(5) // 이미 5회 시도
			.build();
		given(emailVerificationRepository.findById(email)).willReturn(Optional.of(verification));
		// when & then
		assertThatThrownBy(() -> emailService.verifyCode(new VerifyCodeReq(email, "123456"))).isInstanceOf(
			BusinessException.class).extracting("errorCode").isEqualTo(EmailErrorCode.VERIFICATION_MAX_ATTEMPT);
		verify(emailVerificationRepository).deleteById(email);
	}

	@Test
	@DisplayName("인증 실패 - 인증 정보 없음")
	void verifyCode_notFound() {
		// given
		String email = "test@test.com";
		given(emailVerificationRepository.findById(email)).willReturn(Optional.empty());
		// when & then
		assertThatThrownBy(() -> emailService.verifyCode(new VerifyCodeReq(email, "123456"))).isInstanceOf(
			BusinessException.class).extracting("errorCode").isEqualTo(EmailErrorCode.VERIFICATION_NOT_FOUND);
	}

	@Nested
	@DisplayName("당첨자 이메일 발송 (sendWinnerNotifications)")
	class SendWinnerNotificationsTest {

		@Test
		@DisplayName("성공 - 당첨자 3명에게 이메일 발송")
		void success() {
			Long scheduleId = 1L;

			List<LotteryResultEmailInfo> winners = List.of(
				new LotteryResultEmailInfo(1L, 10L, "홍길동", SeatGradeType.VIP, 2, LocalDateTime.now()),
				new LotteryResultEmailInfo(2L, 20L, "김철수", SeatGradeType.ROYAL, 1, LocalDateTime.now()),
				new LotteryResultEmailInfo(3L, 30L, "이영희", SeatGradeType.STANDARD, 4, LocalDateTime.now()));

			given(lotteryResultRepository.findSendEmailInfoByScheduleId(scheduleId)).willReturn(winners);

			given(memberRepository.findById(anyLong())).willAnswer(invocation -> {
				Long id = invocation.getArgument(0);
				return Optional.of(Member.builder().email("user" + id + "@test.com").name("회원" + id).build());
			});

			emailService.sendWinnerNotifications(scheduleId);

			verify(lotteryResultRepository).findSendEmailInfoByScheduleId(scheduleId);
			verify(memberRepository, times(3)).findById(anyLong());
			verify(emailSender, times(3)).sendLotteryWinnerEmail(anyString(), anyString(), any(), anyInt(), any());
		}

		@Test
		@DisplayName("당첨자 없음 - 이메일 발송 안함")
		void noWinners() {
			Long scheduleId = 1L;

			given(lotteryResultRepository.findSendEmailInfoByScheduleId(scheduleId)).willReturn(List.of());

			emailService.sendWinnerNotifications(scheduleId);

			verify(emailSender, never()).sendLotteryWinnerEmail(any(), any(), any(), anyInt(), any());
		}

		@Test
		@DisplayName("일부 실패 - 한 명 실패해도 나머지는 발송됨")
		void partialFailure() {
			Long scheduleId = 1L;

			List<LotteryResultEmailInfo> winners = List.of(
				new LotteryResultEmailInfo(1L, 10L, "홍길동", SeatGradeType.VIP, 2, LocalDateTime.now()),
				new LotteryResultEmailInfo(2L, 999L, "없는회원", SeatGradeType.ROYAL, 1, LocalDateTime.now()));

			given(lotteryResultRepository.findSendEmailInfoByScheduleId(scheduleId)).willReturn(winners);

			given(memberRepository.findById(10L)).willReturn(
				Optional.of(Member.builder().email("hong@test.com").name("홍길동").build()));

			given(memberRepository.findById(999L)).willReturn(Optional.empty());

			emailService.sendWinnerNotifications(scheduleId);

			verify(emailSender, times(1)).sendLotteryWinnerEmail(anyString(), anyString(), any(), anyInt(), any());
		}
	}

	@Nested
	@DisplayName("당첨 취소 이메일 발송 (sendCancelUnpaidNotifications)")
	class SendCancelUnpaidNotificationsTest {

		@Test
		@DisplayName("성공 - 대상자 3명에게 취소 이메일 발송")
		void success() {
			// given
			List<Long> memberIds = List.of(10L, 20L, 30L);

			given(memberRepository.findById(anyLong()))
				.willAnswer(invocation -> {
					Long id = invocation.getArgument(0);
					return Optional.of(
						Member.builder()
							.email("user" + id + "@test.com")
							.name("회원" + id)
							.build()
					);
				});

			// when
			emailService.sendCancelUnpaidNotifications(memberIds);

			// then
			verify(memberRepository, times(3)).findById(anyLong());
			verify(emailSender, times(3))
				.sendCancelUnpaidEmail(anyString(), anyString());
		}

		@Test
		@DisplayName("대상자 없음 - 이메일 발송 안함")
		void noTargets() {
			// when
			emailService.sendCancelUnpaidNotifications(List.of());

			// then
			verify(memberRepository, never()).findById(anyLong());
			verify(emailSender, never())
				.sendCancelUnpaidEmail(anyString(), anyString());
		}

		@Test
		@DisplayName("일부 실패 - 한 명 실패해도 나머지는 발송됨")
		void partialFailure() {
			// given
			List<Long> memberIds = List.of(10L, 999L);

			given(memberRepository.findById(10L))
				.willReturn(Optional.of(
					Member.builder()
						.email("hong@tt.com")
						.name("홍길동")
						.build()
				));

			given(memberRepository.findById(999L))
				.willReturn(Optional.empty());

			// when
			emailService.sendCancelUnpaidNotifications(memberIds);

			// then
			verify(memberRepository, times(2)).findById(anyLong());
			verify(emailSender, times(1))
				.sendCancelUnpaidEmail(anyString(), anyString());
		}
	}

}
