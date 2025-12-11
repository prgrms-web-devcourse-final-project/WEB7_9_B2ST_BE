package com.back.b2st.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserPrincipal {
	private Long id;
	private String email;
	private String role;
}
