package com.back.b2st.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.global.error.code.CommonErrorCode;
import com.back.b2st.global.error.code.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException authException) throws IOException {

		Object exceptionAttribute = request.getAttribute("exception");
		ErrorCode errorCode;

		if (exceptionAttribute instanceof ErrorCode) {
			// 필터가 구체적인 에러(만료, 위조 등)를 잡아낸 경우
			errorCode = (ErrorCode)exceptionAttribute;
		} else {
			// 에러가 없는데 여기로 온 경우 (예: 토큰 없이 접근, 이상한 헤더 등) -> 기본 401
			errorCode = CommonErrorCode.UNAUTHORIZED;
		}

		log.warn("Unauthorized Error: {}", errorCode.getMessage());

		// JSON 응답 생성
		sendErrorResponse(response, errorCode);
	}

	private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		response.setContentType("application/json;charset=UTF-8");
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // HTTP 상태 코드 401 고정

		BaseResponse<Void> responseBody = BaseResponse.error(errorCode);
		response.getWriter().write(objectMapper.writeValueAsString(responseBody));
	}
}
