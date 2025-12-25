package com.back.b2st.global.jpa.entity;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.back.b2st.security.UserPrincipal;

public class SpringSecurityAuditorAware implements AuditorAware<Long> {
	@Override
	public Optional<Long> getCurrentAuditor() {
		// 인증 정보 받아오기
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		/**
		 * 인증 정보 없음, 인증되지 않은 사용자, 익명 사용자
		 */
		if (authentication == null ||
			!authentication.isAuthenticated() ||
			authentication instanceof AnonymousAuthenticationToken) {
			return Optional.empty();
		}

		Object principal = authentication.getPrincipal();

		if (principal instanceof UserPrincipal userPrincipal) {
			return Optional.of(userPrincipal.getId());
		}

		return Optional.empty();
	}
}
