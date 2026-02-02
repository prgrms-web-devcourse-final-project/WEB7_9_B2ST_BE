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
import com.back.b2st.global.test.AbstractContainerBaseTest;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemberControllerTest extends AbstractContainerBaseTest {

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

			mockMvc.perform(post("/api/members/signup")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))
					.header("X-Forwarded-For", "192.168.1.100"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(201));
		}

		@Test
		@DisplayName("실패 - 유효하지 않은 이메일 형식")
		void fail_badEmail() throws Exception {
			SignupReq request = createSignupRequest("bad-email", "StrongP@ss123", "신규유저");

			mockMvc.perform(post("/api/members/signup")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))
					.header("X-Forwarded-For", "192.168.1.100"))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("실패 - 비밀번호 규칙 미준수")
		void fail_weakPassword() throws Exception {
			SignupReq request = createSignupRequest("user@test.com", "weakpassword1", "신규유저");

			mockMvc.perform(post("/api/members/signup")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))
					.header("X-Forwarded-For", "192.168.1.100"))
				.andDo(print())
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("회원가입 Rate Limit 초과")
		void signup_RateLimitExceeded() throws Exception {
			// given: Rate Limit 3회 초과
			SignupReq request = createSignupRequest("newuser@test.com", "StrongP@ss123", "신규유저");

			String testIp = "192.168.1.200";
			// 3번 성공 (각각 다른 이메일로)
			for (int i = 1; i <= 3; i++) {
				SignupReq uniqueReq = new SignupReq(
					"newuser" + i + "@test.com",
					"StrongP@ss123",
					"신규유저",
					"0101234567" + i,
					LocalDate.of(1990, 1, 1));
				mockMvc.perform(post("/api/members/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(uniqueReq))
						.header("X-Forwarded-For", testIp))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value(201));
			}
			// when & then: 4번째 시도 - Rate Limit 초과
			mockMvc.perform(post("/api/members/signup")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))
					.header("X-Forwarded-For", testIp))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.message").value("가입 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."));
		}
	}
}
