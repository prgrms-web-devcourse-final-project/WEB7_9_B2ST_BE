package com.back.b2st.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.global.error.code.CommonErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException authException) throws IOException {

		// 유효 토큰 없이 접근하려 할 때 401 응답
		response.setContentType("application/json;charset=UTF-8");
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

		// CommonErrorCode.UNAUTHORIZED 또는 AuthErrorCode를 사용하여 JSON 응답 생성
		BaseResponse<Void> errorResponse = BaseResponse.error(CommonErrorCode.UNAUTHORIZED);

		response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
	}
}
