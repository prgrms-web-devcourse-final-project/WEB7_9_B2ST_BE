package com.back.b2st.security;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.global.error.code.CommonErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
		AccessDeniedException accessDeniedException) throws IOException {

		log.warn("Forbidden Error: {}", accessDeniedException.getMessage());

		// 헤더 설정
		response.setContentType("application/json;charset=UTF-8");

		// 상태 코드
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);

		// 응답 내용
		BaseResponse<Void> responseBody = BaseResponse.error(CommonErrorCode.FORBIDDEN);

		response.getWriter().write(objectMapper.writeValueAsString(responseBody));
	}
}