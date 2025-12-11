package com.back.b2st.domain.member.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.auth.dto.LoginRequest;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.global.test.AbstractContainerBaseTest;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class MypageControllerTest extends AbstractContainerBaseTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void setup() {
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("통합: 내 정보 조회 성공")
	void getMyInfo_success() throws Exception {
		String email = "mypage@test.com";
		Member member = Member.builder()
			.email(email)
			.password(passwordEncoder.encode("Password123!"))
			.name("마이페이지")
			.role(Member.Role.MEMBER)
			.provider(Member.Provider.EMAIL)
			.isVerified(true)
			.build();
		memberRepository.save(member);

		// 토큰 발급
		LoginRequest loginRequest = new LoginRequest();
		ReflectionTestUtils.setField(loginRequest, "email", email);
		ReflectionTestUtils.setField(loginRequest, "password", "Password123!");

		String loginResponse = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andReturn().getResponse().getContentAsString();

		// 토큰 추출
		JsonNode jsonNode = objectMapper.readTree(loginResponse);
		String accessToken = jsonNode.path("data").path("accessToken").asText();

		// 내 정보 조회 요청
		mockMvc.perform(get("/mypage/me")
				.header("Authorization", "Bearer " + accessToken) // 토큰 필수
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			// 응답 검증
			.andExpect(jsonPath("$.data.email").value(email))
			.andExpect(jsonPath("$.data.name").value("마이페이지"));
	}

	@Test
	@DisplayName("통합: 내 정보 조회 실패 - 토큰 없음")
	void getMyInfo_fail_no_token() throws Exception {
		mockMvc.perform(get("/mypage/me")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isForbidden());
	}
}
