package com.back.b2st.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.io.IOException;
import java.io.PrintWriter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.back.b2st.domain.auth.error.AuthErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
public class JwtAccessDeniedHandlerTest {

	@InjectMocks
	private JwtAccessDeniedHandler jwtAccessDeniedHandler;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private PrintWriter writer;

	private ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@DisplayName("권한 없는 접근 시 403 상태코드와 정확한 에러 코드를 반환한다")
	void handle_shouldReturn403AndCorrectErrorCode() throws IOException {
		// given
		given(response.getWriter()).willReturn(writer);
		AccessDeniedException ex = new AccessDeniedException("Access Denied");

		// when
		jwtAccessDeniedHandler.handle(request, response, ex);

		// then
		verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
		verify(response).setContentType("application/json;charset=UTF-8");

		ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(writer).write(jsonCaptor.capture());

		// JsonNode로 파싱
		String actualJson = jsonCaptor.getValue();
		JsonNode rootNode = objectMapper.readTree(actualJson);

		String actualMessage = rootNode.path("message").asText();

		verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
		verify(response).setStatus(AuthErrorCode.UNAUTHORIZED_ACCESS.getStatus().value());
		assertEquals(AuthErrorCode.UNAUTHORIZED_ACCESS.getMessage(), actualMessage);
	}
}
