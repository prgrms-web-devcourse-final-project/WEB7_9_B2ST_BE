package com.back.b2st.global.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.internet.MimeMessage;

/**
 * 테스트 환경에서 JavaMailSender를 Mock으로 대체
 * 실제 SMTP 서버 연결 없이 테스트 가능
 */
@TestConfiguration
public class TestMailConfig {

	@Bean
	@Primary
	public JavaMailSender javaMailSender() {
		JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
		MimeMessage mimeMessage = Mockito.mock(MimeMessage.class);
		Mockito.when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
		return mailSender;
	}
}
