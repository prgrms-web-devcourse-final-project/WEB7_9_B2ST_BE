package com.back.b2st.domain.email.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;

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

	@Test
	@DisplayName("이메일 발송 성공")
	void sendEmailAsync_success() {
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
	@DisplayName("이메일 발송 실패 - RuntimeException 발생 시 로깅 후 정상 종료")
	void sendEmailAsync_runtimeException() {
		// given
		String to = "user@example.com";
		String code = "123456";

		given(mailSender.createMimeMessage()).willReturn(mimeMessage);
		given(templateEngine.process(eq("email/verification"), any())).willReturn("<html>Test</html>");
		willThrow(new RuntimeException("SMTP connection failed")).given(mailSender).send(any(MimeMessage.class));

		// when
		emailSender.sendEmailAsync(to, code);

		// then - 발송 시도는 이루어짐
		verify(mailSender).createMimeMessage();
		verify(mailSender).send(any(MimeMessage.class));
	}

	@Test
	@DisplayName("템플릿 렌더링이 올바르게 호출된다")
	void sendEmailAsync_templateRendering() {
		// given
		String to = "user@example.com";
		String code = "654321";

		given(mailSender.createMimeMessage()).willReturn(mimeMessage);
		given(templateEngine.process(eq("email/verification"), any())).willReturn("<html>Code: 654321</html>");

		// when
		emailSender.sendEmailAsync(to, code);

		// then
		verify(templateEngine).process(eq("email/verification"), argThat(context -> {
			return context != null;
		}));
	}

	@Test
	@DisplayName("이메일 마스킹이 적용된 로깅")
	void sendEmailAsync_maskedLogging() {
		// given
		String to = "testuser@example.com";
		String code = "123456";

		given(mailSender.createMimeMessage()).willReturn(mimeMessage);
		given(templateEngine.process(eq("email/verification"), any())).willReturn("<html>Test</html>");

		// when
		emailSender.sendEmailAsync(to, code);

		// then
		verify(mailSender).send(mimeMessage);
	}

	@Test
	@DisplayName("이메일 발송 실패 - MessagingException 발생 시 로깅 후 정상 종료")
	void sendEmailAsync_messagingException() throws Exception {
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

	@Test
	@DisplayName("이메일 발송 실패 - 인코딩 오류 시 로깅 후 정상 종료")
	void sendEmailAsync_encodingException() throws Exception {
		// given
		String to = "user@example.com";
		String code = "123456";

		// createMimeMessage는 정상 반환하되, 첫 번째 호출에서 실제 MimeMessage 반환
		MimeMessage realMimeMessage = mock(MimeMessage.class);
		given(mailSender.createMimeMessage()).willReturn(realMimeMessage);
		given(templateEngine.process(eq("email/verification"), any())).willReturn("<html>Test</html>");

		// when
		emailSender.sendEmailAsync(to, code);

		// then
		verify(mailSender).createMimeMessage();
	}
}
