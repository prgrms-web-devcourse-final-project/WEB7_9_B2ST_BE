package com.back.b2st.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.back.b2st.global.jwt.JwtAuthenticationFilter;
import com.back.b2st.global.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 메서드 수준 보안을 활성화
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtTokenProvider jwtTokenProvider;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.csrf(AbstractHttpConfigurer::disable)
			.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

			.authorizeHttpRequests(auth -> auth
				// 관리자 전용 경로 (URL 레벨 보호)
				.requestMatchers("/api/admin/**").hasRole("ADMIN")
				// 대기열 API - 인증 필요 (로그인 사용자만 접근 가능)
				.requestMatchers("/api/queues/**").authenticated()
				// Actuator (개발/모니터링용)
				.requestMatchers(
					"/actuator/health",
					"/actuator/info",
					"/actuator/scheduledtasks",
					"/actuator/circuitbreakers",        // Circuit Breaker 상태
					"/actuator/circuitbreakerevents"    // Circuit Breaker 이벤트
				).permitAll()
				// 인증 필요한 auth 하위 경로 (link, logout)
				.requestMatchers("/api/auth/link/**", "/api/auth/logout").authenticated()
				// 공개 경로
				.requestMatchers(
					"/api/members/signup", "/api/auth/**", "/h2-console/**", "/error", "/api/banks",
					"/api/email/**",
					"/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html" // Swagger
				).permitAll()
				// 나머지 모든 요청은 인증 필요
				.anyRequest().authenticated())
			.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint(jwtAuthenticationEntryPoint) // 401 에러 처리
				.accessDeniedHandler(jwtAccessDeniedHandler) // 403 에러 처리
			)
			.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
				UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		// 프엔주소
		configuration.setAllowedOrigins(List.of(
			"http://localhost:3000",
			"https://b2st.doncrytt.online",
			"https://api.b2st.doncrytt.online",
			"https://www.doncrytt.online",
			"https://doncrytt.vercel.app"));

		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		// 쿠키나 인증헤더
		configuration.setAllowCredentials(true);
		// 캐시 시간 1시간
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}