package com.back.b2st.global.jpa.entity;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;

public class SpringSecurityAuditorAware implements AuditorAware<Long> {
	@Override
	public Optional<Long> getCurrentAuditor() {
		// TODO SpringSecurity 관련 작업 이후 진행
		return Optional.empty();
	}
}
