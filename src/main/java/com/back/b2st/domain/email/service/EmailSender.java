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
	@Value("${app.mail.from-name:B2ST 티켓팅}")
	private String fromName;

	@Async("emailExecutor")
	public void sendEmailAsync(String to, String code) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			helper.setFrom(fromAddress, fromName);
			helper.setTo(to);
			helper.setSubject("[TT] 이메일 인증 코드");

			// 타임리프 템플릿 렌더링. html포맷만 써도 될 지도
			String htmlContent = renderEmailTemplate(code);
			helper.setText(htmlContent, true); // true = html

			mailSender.send(message);

			log.info("이메일 발송 성공: to={}", maskEmail(to));

		} catch (MessagingException e) {
			log.error("이메일 발송 실패: to={}, error={}", maskEmail(to), e.getMessage(), e);
		} catch (java.io.UnsupportedEncodingException e) {
			log.error("이메일 인코딩 오류: {}", e.getMessage(), e);
		} catch (Exception e) {
			log.error("이메일 발송 중 예상치 못한 오류: to={}, error={}", maskEmail(to), e.getMessage(), e);
		}
	}

	// 이메일 템플릿 렌더링
	private String renderEmailTemplate(String code) {
		Context context = new Context();
		context.setVariable("code", code);
		context.setVariable("expireMinutes", 5);

		return this.templateEngine.process("email/verification", context);
	}
}
