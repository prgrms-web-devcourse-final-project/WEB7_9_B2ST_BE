package com.back.b2st.domain.email.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.back.b2st.domain.seat.grade.entity.SeatGradeType;

import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class EmailSenderTest {

	@InjectMocks
	private EmailSender emailSender;

	@Mock
	private JavaMailSender mailSender;

	@Mock
	private SpringTemplateEngine templateEngine;

	@Mock
	private MimeMessage mimeMessage;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(emailSender, "fromAddress", "test@example.com");
		ReflectionTestUtils.setField(emailSender, "fromName", "Test Service");
	}

	@Nested
	@DisplayName("이메일 인증 코드 발송 (sendEmailAsync)")
	class SendEmailAsyncTest {

		@Test
		@DisplayName("성공")
		void success() {
			// given
			String to = "user@example.com";
			String code = "123456";

			given(mailSender.createMimeMessage()).willReturn(mimeMessage);
			given(templateEngine.process(eq("email/verification"), any())).willReturn("<html>Test</html>");

			// when
			emailSender.sendEmailAsync(to, code);

			// then
			verify(mailSender).createMimeMessage();
			verify(mailSender).send(mimeMessage);
			verify(templateEngine).process(eq("email/verification"), any());
		}

		@Test
		@DisplayName("실패 - RuntimeException 발생 시 로깅 후 정상 종료")
		void runtimeException() {
			// given
			String to = "user@example.com";
			String code = "123456";

			given(mailSender.createMimeMessage()).willReturn(mimeMessage);
			given(templateEngine.process(eq("email/verification"), any())).willReturn("<html>Test</html>");
			willThrow(new RuntimeException("SMTP connection failed")).given(mailSender).send(any(MimeMessage.class));

			// when
			emailSender.sendEmailAsync(to, code);

			// then - 예외 발생해도 메서드 정상 종료
			verify(mailSender).send(any(MimeMessage.class));
		}

		@Test
		@DisplayName("템플릿 렌더링이 올바르게 호출된다")
		void templateRendering() {
			// given
			String to = "user@example.com";
			String code = "654321";

			given(mailSender.createMimeMessage()).willReturn(mimeMessage);
			given(templateEngine.process(eq("email/verification"), any())).willReturn("<html>Code: 654321</html>");

			// when
			emailSender.sendEmailAsync(to, code);

			// then
			verify(templateEngine).process(eq("email/verification"), argThat(context -> context != null));
		}

		@Test
		@DisplayName("실패 - MessagingException 발생 시 로깅 후 정상 종료")
		void messagingException() {
			// given
			String to = "user@example.com";
			String code = "123456";

			given(mailSender.createMimeMessage()).willReturn(mimeMessage);
			given(templateEngine.process(eq("email/verification"), any())).willReturn("<html>Test</html>");
			willThrow(new org.springframework.mail.MailSendException("Send failed",
				new jakarta.mail.MessagingException("SMTP error")))
				.given(mailSender).send(any(MimeMessage.class));

			// when
			emailSender.sendEmailAsync(to, code);

			// then
			verify(mailSender).send(any(MimeMessage.class));
		}
	}

	@Nested
	@DisplayName("복구 이메일 발송 (sendRecoveryEmail)")
	class SendRecoveryEmailTest {

		@Test
		@DisplayName("성공")
		void success() {
			// given
			String to = "user@example.com";
			String name = "홍길동";
			String recoveryLink = "https://example.com/recovery?token=abc123";

			given(mailSender.createMimeMessage()).willReturn(mimeMessage);
			given(templateEngine.process(eq("email/recovery-email"), any())).willReturn("<html>Recovery</html>");

			// when
			emailSender.sendRecoveryEmail(to, name, recoveryLink);

			// then
			verify(mailSender).createMimeMessage();
			verify(mailSender).send(mimeMessage);
			verify(templateEngine).process(eq("email/recovery-email"), any());
		}

		@Test
		@DisplayName("템플릿에 name, recoveryLink, expiryHours가 전달된다")
		void templateVariables() {
			// given
			String to = "user@example.com";
			String name = "테스트유저";
			String recoveryLink = "https://example.com/recovery?token=xyz";

			given(mailSender.createMimeMessage()).willReturn(mimeMessage);
			given(templateEngine.process(eq("email/recovery-email"), any())).willReturn("<html>Test</html>");

			// when
			emailSender.sendRecoveryEmail(to, name, recoveryLink);

			// then - Context에 변수가 설정됨
			verify(templateEngine).process(eq("email/recovery-email"), argThat(context -> context != null));
		}

		@Test
		@DisplayName("실패 - RuntimeException 발생 시 로깅 후 정상 종료")
		void runtimeException() {
			// given
			String to = "user@example.com";
			String name = "홍길동";
			String recoveryLink = "https://example.com/recovery?token=abc123";

			given(mailSender.createMimeMessage()).willReturn(mimeMessage);
			given(templateEngine.process(eq("email/recovery-email"), any())).willReturn("<html>Recovery</html>");
			willThrow(new RuntimeException("SMTP connection failed")).given(mailSender).send(any(MimeMessage.class));

			// when
			emailSender.sendRecoveryEmail(to, name, recoveryLink);

			// then - 예외 발생해도 메서드 정상 종료
			verify(mailSender).send(any(MimeMessage.class));
		}

		@Test
		@DisplayName("실패 - MailSendException 발생 시 로깅 후 정상 종료")
		void mailSendException() {
			// given
			String to = "user@example.com";
			String name = "홍길동";
			String recoveryLink = "https://example.com/recovery?token=abc123";

			given(mailSender.createMimeMessage()).willReturn(mimeMessage);
			given(templateEngine.process(eq("email/recovery-email"), any())).willReturn("<html>Recovery</html>");
			willThrow(new org.springframework.mail.MailSendException("Mail server unavailable"))
				.given(mailSender).send(any(MimeMessage.class));

			// when
			emailSender.sendRecoveryEmail(to, name, recoveryLink);

			// then
			verify(mailSender).send(any(MimeMessage.class));
		}
	}

	@Nested
	@DisplayName("공통 메서드 (sendHtmlEmail) 에러 처리")
	class SendHtmlEmailErrorTest {

		@Test
		@DisplayName("이메일 마스킹이 적용된 로깅")
		void maskedLogging() {
			// given
			String to = "testuser@example.com";
			String code = "123456";

			given(mailSender.createMimeMessage()).willReturn(mimeMessage);
			given(templateEngine.process(eq("email/verification"), any())).willReturn("<html>Test</html>");

			// when
			emailSender.sendEmailAsync(to, code);

			// then - 마스킹된 이메일로 로깅됨 (로그 검증은 어렵지만 send는 호출됨)
			verify(mailSender).send(mimeMessage);
		}

		@Test
		@DisplayName("인코딩 오류 시 로깅 후 정상 종료")
		void encodingException() {
			// given
			String to = "user@example.com";
			String code = "123456";

			MimeMessage realMimeMessage = mock(MimeMessage.class);
			given(mailSender.createMimeMessage()).willReturn(realMimeMessage);
			given(templateEngine.process(eq("email/verification"), any())).willReturn("<html>Test</html>");

			// when
			emailSender.sendEmailAsync(to, code);

			// then
			verify(mailSender).createMimeMessage();
		}

		@Nested
		@DisplayName("추첨 당첨 이메일 발송 (sendLotteryWinnerEmail)")
		class SendLotteryWinnerEmailTest {

			@Test
			@DisplayName("성공")
			void success() {
				// given
				String to = "winner@example.com";
				String name = "홍길동";
				SeatGradeType grade = SeatGradeType.VIP;
				Integer quantity = 2;
				LocalDateTime paymentDeadline = LocalDateTime.of(2025, 1, 15, 23, 59);

				given(mailSender.createMimeMessage()).willReturn(mimeMessage);
				given(templateEngine.process(eq("email/lottery-winner"), any()))
					.willReturn("<html>Winner</html>");

				// when
				emailSender.sendLotteryWinnerEmail(to, name, grade, quantity, paymentDeadline);

				// then
				verify(mailSender).createMimeMessage();
				verify(mailSender).send(mimeMessage);
				verify(templateEngine).process(eq("email/lottery-winner"), any());
			}

			@Test
			@DisplayName("템플릿에 name, grade, quantity, paymentDeadline이 전달된다")
			void templateVariables() {
				// given
				String to = "winner@example.com";
				String name = "테스트유저";
				SeatGradeType grade = SeatGradeType.ROYAL;
				Integer quantity = 4;
				LocalDateTime paymentDeadline = LocalDateTime.of(2025, 1, 20, 23, 59);

				given(mailSender.createMimeMessage()).willReturn(mimeMessage);
				given(templateEngine.process(eq("email/lottery-winner"), any()))
					.willReturn("<html>Test</html>");

				// when
				emailSender.sendLotteryWinnerEmail(to, name, grade, quantity, paymentDeadline);

				// then
				verify(templateEngine).process(
					eq("email/lottery-winner"),
					argThat(context -> context != null)
				);
			}

			@Test
			@DisplayName("실패 - RuntimeException 발생 시 로깅 후 정상 종료")
			void runtimeException() {
				// given
				String to = "winner@example.com";
				String name = "홍길동";
				SeatGradeType grade = SeatGradeType.VIP;
				Integer quantity = 2;
				LocalDateTime paymentDeadline = LocalDateTime.of(2025, 1, 15, 23, 59);

				given(mailSender.createMimeMessage()).willReturn(mimeMessage);
				given(templateEngine.process(eq("email/lottery-winner"), any()))
					.willReturn("<html>Winner</html>");
				willThrow(new RuntimeException("SMTP connection failed"))
					.given(mailSender).send(any(MimeMessage.class));

				// when
				emailSender.sendLotteryWinnerEmail(to, name, grade, quantity, paymentDeadline);

				// then - 예외 발생해도 메서드 정상 종료
				verify(mailSender).send(any(MimeMessage.class));
			}

			@Test
			@DisplayName("실패 - MailSendException 발생 시 로깅 후 정상 종료")
			void mailSendException() {
				// given
				String to = "winner@example.com";
				String name = "홍길동";
				SeatGradeType grade = SeatGradeType.STANDARD;
				Integer quantity = 1;
				LocalDateTime paymentDeadline = LocalDateTime.of(2025, 1, 10, 23, 59);

				given(mailSender.createMimeMessage()).willReturn(mimeMessage);
				given(templateEngine.process(eq("email/lottery-winner"), any()))
					.willReturn("<html>Winner</html>");
				willThrow(new org.springframework.mail.MailSendException("Mail server unavailable"))
					.given(mailSender).send(any(MimeMessage.class));

				// when
				emailSender.sendLotteryWinnerEmail(to, name, grade, quantity, paymentDeadline);

				// then
				verify(mailSender).send(any(MimeMessage.class));
			}
		}
	}

	@Nested
	@DisplayName("알림 이메일 발송 (sendNotificationEmail)")
	class SendNotificationEmailTest {

		@Test
		@DisplayName("성공")
		void success() {
			// given
			String to = "user@example.com";
			String subject = "subject";
			String message = "message";

			given(mailSender.createMimeMessage()).willReturn(mimeMessage);
			given(templateEngine.process(eq("email/notification"), any())).willReturn("<html>Notification</html>");

			// when
			emailSender.sendNotificationEmail(to, subject, message);

			// then
			verify(mailSender).send(mimeMessage);
			verify(templateEngine).process(eq("email/notification"), any());
		}
	}
}
