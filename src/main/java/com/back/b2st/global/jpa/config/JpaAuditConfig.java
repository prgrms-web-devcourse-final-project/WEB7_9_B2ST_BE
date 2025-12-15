package com.back.b2st.global.jpa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.back.b2st.global.jpa.entity.SpringSecurityAuditorAware;

@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {
	@Bean
	public AuditorAware<Long> auditorProvider() {
		return new SpringSecurityAuditorAware();
	}
}
