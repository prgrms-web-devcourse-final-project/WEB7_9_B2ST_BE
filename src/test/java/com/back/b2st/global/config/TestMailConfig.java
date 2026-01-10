package com.back.b2st.global.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.internet.MimeMessage;

@Configuration
@Profile("test")
public class TestMailConfig {

	@Bean
	@Primary
	public JavaMailSender javaMailSender() {
		JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
		MimeMessage mimeMessage = Mockito.mock(MimeMessage.class);

		Mockito.when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
		// send()는 void라 기본이 doNothing()이라 SMTP 연결 자체를 안 함
		return mailSender;
	}
}
