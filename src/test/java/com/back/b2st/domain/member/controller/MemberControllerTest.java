package com.back.b2st.domain.member.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.member.dto.request.SignupReq;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemberControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	// 헬퍼 메서드
	private SignupReq createSignupRequest(String email, String pw, String name) {
		return new SignupReq(email, pw, name, "01012345678", LocalDate.of(1990, 1, 1));
	}

	@Nested
	@DisplayName("회원가입 API")
	class SignupTest {

		@Test
		@DisplayName("성공")
		void success() throws Exception {
			SignupReq request = createSignupRequest("newuser@test.com", "StrongP@ss123", "신규유저");

			mockMvc.perform(post("/api/members/signup").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andDo(print()).andExpect(status().isOk());
		}

		@Test
		@DisplayName("실패 - 유효하지 않은 이메일 형식")
		void fail_badEmail() throws Exception {
			SignupReq request = createSignupRequest("bad-email", "StrongP@ss123", "신규유저");

			mockMvc.perform(post("/api/members/signup").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andDo(print()).andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("실패 - 비밀번호 규칙 미준수")
		void fail_weakPassword() throws Exception {
			SignupReq request = createSignupRequest("user@test.com", "weakpassword1", "신규유저");

			mockMvc.perform(post("/api/members/signup").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andDo(print()).andExpect(status().isBadRequest());
		}
	}
}
