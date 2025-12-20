package com.back.b2st.domain.email.service;

import static com.back.b2st.global.util.MaskingUtil.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSender {

	private final JavaMailSender mailSender;
	private final SpringTemplateEngine templateEngine;

	@Value("${app.mail.from-address:noreply@b2st.com}")
	private String fromAddress;
	@Value("${app.mail.from-name:B2ST TT}")
	private String fromName;

	@Async("emailExecutor")
	public void sendEmailAsync(String to, String code) {
		Context context = new Context();
		context.setVariable("code", code);
		context.setVariable("expireMinutes", 5);

		sendHtmlEmail(to, "[TT] 이메일 인증 코드", "email/verification", context);
	}

	@Async("emailExecutor")
	public void sendRecoveryEmail(String to, String name, String recoveryLink) {
		Context context = new Context();
		context.setVariable("name", name);
		context.setVariable("recoveryLink", recoveryLink);
		context.setVariable("expiryHours", 24);

		sendHtmlEmail(to, "[TT] 계정 복구 안내", "email/recovery-email", context);
	}

	// 이메일 발송 헬퍼
	private void sendHtmlEmail(String to, String subject, String templateName, Context context) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			helper.setFrom(fromAddress, fromName);
			helper.setTo(to);
			helper.setSubject(subject);

			String htmlContent = templateEngine.process(templateName, context);
			helper.setText(htmlContent, true);

			mailSender.send(message);

			log.info("이메일 발송 성공: to={}, subject={}", maskEmail(to), subject);

		} catch (MessagingException e) {
			log.error("이메일 발송 실패: to={}, error={}", maskEmail(to), e.getMessage(), e);
		} catch (java.io.UnsupportedEncodingException e) {
			log.error("이메일 인코딩 오류: {}", e.getMessage(), e);
		} catch (Exception e) {
			log.error("이메일 발송 중 예상치 못한 오류: to={}, error={}", maskEmail(to), e.getMessage(), e);
		}
	}
}
