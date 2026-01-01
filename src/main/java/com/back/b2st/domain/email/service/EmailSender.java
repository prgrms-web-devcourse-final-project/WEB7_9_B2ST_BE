package com.back.b2st.domain.email.service;

import static com.back.b2st.global.util.MaskingUtil.*;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.back.b2st.domain.seat.grade.entity.SeatGradeType;

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
		sendTemplateEmail(to, "[TT] 이메일 인증 코드", "email/verification",
			Map.of(
				"code", code,
				"expireMinutes", 5
			));
	}

	@Async("emailExecutor")
	public void sendRecoveryEmail(String to, String name, String recoveryLink) {
		sendTemplateEmail(to, "[TT] 계정 복구 안내", "email/recovery-email",
			Map.of(
				"name", name,
				"recoveryLink", recoveryLink,
				"expiryHours", 24
			));
	}

	@Async("emailExecutor")
	public void sendLotteryWinnerEmail(
		String to,
		String name,
		SeatGradeType grade,
		Integer quantity,
		LocalDateTime paymentDeadline
	) {
		sendTemplateEmail(to, "[TT] 추첨 당첨 안내", "email/lottery-winner",
			Map.of(
				"name", name,
				"grade", grade,
				"quantity", quantity,
				"paymentDeadline", paymentDeadline
			));
	}

	@Async("emailExecutor")
	public void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> variables) {
		Context context = new Context();
		if (variables != null) {
			variables.forEach(context::setVariable);
		}
		sendHtmlEmail(to, subject, templateName, context);
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
