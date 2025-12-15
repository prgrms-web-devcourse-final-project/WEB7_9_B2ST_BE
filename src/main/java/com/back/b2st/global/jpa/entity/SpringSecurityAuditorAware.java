package com.back.b2st.global.jpa.entity;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.back.b2st.security.CustomUserDetails;

public class SpringSecurityAuditorAware implements AuditorAware<Long> {
	@Override
	public Optional<Long> getCurrentAuditor() {

		return Optional.ofNullable(SecurityContextHolder.getContext())
			.map(SecurityContext::getAuthentication)
			.filter(Authentication::isAuthenticated)
			.map(Authentication::getPrincipal)
			.filter(CustomUserDetails.class::isInstance)
			.map(CustomUserDetails.class::cast)
			.map(CustomUserDetails::getId);
	}
}
