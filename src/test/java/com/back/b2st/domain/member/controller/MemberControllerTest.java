package com.back.b2st.domain.member.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.member.dto.SignupRequest;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemberControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper; // JSON 변환용

	@Test
	@DisplayName("통합: 회원가입 성공")
	void signup_integration_success() throws Exception {
		// given
		SignupRequest request = createSignupRequest("newuser@test.com", "StrongP@ss123", "신규유저", "NewNick");

		// when & then
		mockMvc.perform(post("/members/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("통합: 회원가입 실패 - 유효하지 않은 이메일 형식")
	void signup_integration_fail_bad_email() throws Exception {
		// given
		SignupRequest request = createSignupRequest("bad-email", "StrongP@ss123", "신규유저", "NewNick");

		// when & then
		mockMvc.perform(post("/members/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("통합: 회원가입 실패 - 비밀번호 규칙 미준수")
	void signup_integration_fail_weak_password() throws Exception {
		// given (특문 미포함)
		SignupRequest request = createSignupRequest("user@test.com", "weakpassword1", "신규유저", "NewNick");

		// when & then
		mockMvc.perform(post("/members/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isBadRequest());
	}

	private SignupRequest createSignupRequest(String email, String pw, String name, String nick) {
		SignupRequest request = new SignupRequest();
		ReflectionTestUtils.setField(request, "email", email);
		ReflectionTestUtils.setField(request, "password", pw);
		ReflectionTestUtils.setField(request, "name", name);
		ReflectionTestUtils.setField(request, "nickname", nick);
		ReflectionTestUtils.setField(request, "birth", LocalDate.of(1990, 1, 1));
		return request;
	}
}
