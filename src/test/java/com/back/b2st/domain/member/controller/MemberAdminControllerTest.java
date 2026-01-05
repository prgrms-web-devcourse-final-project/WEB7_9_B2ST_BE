package com.back.b2st.domain.member.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.auth.dto.request.LoginReq;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.global.test.AbstractContainerBaseTest;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemberAdminControllerTest extends AbstractContainerBaseTest {

	private static final String ADMIN_PASSWORD = "AdminPass123!";
	private static final String MEMBER_PASSWORD = "MemberPass123!";
	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private ObjectMapper objectMapper;
	private Member testMember;
	private Member adminMember;
	private String adminToken;
	private String memberToken;

	@BeforeEach
	void setUp() throws Exception {
		// 테스트 회원 생성
		testMember = Member.builder()
				.email("member@test.com")
				.password(passwordEncoder.encode(MEMBER_PASSWORD))
				.name("테스트회원")
				.phone("01012345678")
				.birth(LocalDate.of(1990, 1, 1))
				.role(Member.Role.MEMBER)
				.provider(Member.Provider.EMAIL)
				.isEmailVerified(true)
				.isIdentityVerified(false)
				.build();
		memberRepository.save(testMember);

		// 관리자 회원 생성
		adminMember = Member.builder()
				.email("admin@test.com")
				.password(passwordEncoder.encode(ADMIN_PASSWORD))
				.name("관리자")
				.phone("01087654321")
				.birth(LocalDate.of(1985, 1, 1))
				.role(Member.Role.ADMIN)
				.provider(Member.Provider.EMAIL)
				.isEmailVerified(true)
				.isIdentityVerified(false)
				.build();
		memberRepository.save(adminMember);

		// JWT 토큰 발급
		adminToken = getAccessToken("admin@test.com", ADMIN_PASSWORD);
		memberToken = getAccessToken("member@test.com", MEMBER_PASSWORD);
	}

	/**
	 * 로그인 후 AccessToken 획득
	 */
	private String getAccessToken(String email, String password) throws Exception {
		LoginReq loginReq = new LoginReq(email, password);
		String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginReq))).andReturn().getResponse().getContentAsString();

		return objectMapper.readTree(response).path("data").path("accessToken").asString();
	}

	@Nested
	@DisplayName("회원 목록 조회 API")
	class GetMembersTest {

		@Test
		@DisplayName("성공 - 관리자 권한")
		void success_withAdminRole() throws Exception {
			mockMvc.perform(get("/api/admin/members").header("Authorization", "Bearer " + adminToken)
					.contentType(MediaType.APPLICATION_JSON))
					.andDo(print())
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value(200))
					.andExpect(jsonPath("$.data.content").isArray())
					.andExpect(jsonPath("$.data.content.length()").value(2)); // testMember + adminMember
		}

		@Test
		@DisplayName("성공 - 이메일 검색")
		void success_searchByEmail() throws Exception {
			mockMvc.perform(get("/api/admin/members").header("Authorization", "Bearer " + adminToken)
					.param("email", "member")
					.contentType(MediaType.APPLICATION_JSON))
					.andDo(print())
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value(200))
					.andExpect(jsonPath("$.data.content.length()").value(1))
					.andExpect(jsonPath("$.data.content[0].email").value("member@test.com"));
		}

		@Test
		@DisplayName("성공 - Role 필터")
		void success_filterByRole() throws Exception {
			mockMvc.perform(get("/api/admin/members").header("Authorization", "Bearer " + adminToken)
					.param("role", "ADMIN")
					.contentType(MediaType.APPLICATION_JSON))
					.andDo(print())
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value(200))
					.andExpect(jsonPath("$.data.content.length()").value(1))
					.andExpect(jsonPath("$.data.content[0].role").value("ADMIN"));
		}

		@Test
		@DisplayName("실패 - 일반 회원 권한")
		void fail_withMemberRole() throws Exception {
			mockMvc.perform(get("/api/admin/members").header("Authorization", "Bearer " + memberToken)
					.contentType(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isForbidden());
		}

		@Test
		@DisplayName("실패 - 미인증")
		void fail_unauthenticated() throws Exception {
			mockMvc.perform(get("/api/admin/members").contentType(MediaType.APPLICATION_JSON))
					.andDo(print())
					.andExpect(status().isUnauthorized());
		}
	}

	@Nested
	@DisplayName("회원 상세 조회 API")
	class GetMemberDetailTest {

		@Test
		@DisplayName("성공")
		void success() throws Exception {
			mockMvc.perform(
					get("/api/admin/members/{memberId}", testMember.getId())
							.header("Authorization", "Bearer " + adminToken)
							.contentType(MediaType.APPLICATION_JSON))
					.andDo(print())
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value(200))
					.andExpect(jsonPath("$.data.id").value(testMember.getId()))
					.andExpect(jsonPath("$.data.email").value("member@test.com"))
					.andExpect(jsonPath("$.data.name").value("테스트회원"));
		}

		@Test
		@DisplayName("실패 - 존재하지 않는 회원")
		void fail_memberNotFound() throws Exception {
			mockMvc.perform(
					get("/api/admin/members/{memberId}", 999999L).header("Authorization", "Bearer " + adminToken)
							.contentType(MediaType.APPLICATION_JSON))
					.andDo(print())
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.code").value(404));
		}

		@Test
		@DisplayName("실패 - 일반 회원 권한")
		void fail_withMemberRole() throws Exception {
			mockMvc.perform(get("/api/admin/members/{memberId}", testMember.getId()).header("Authorization",
					"Bearer " + memberToken).contentType(MediaType.APPLICATION_JSON))
					.andDo(print())
					.andExpect(status().isForbidden());
		}
	}

	@Nested
	@DisplayName("대시보드 통계 API")
	class GetDashboardStatsTest {

		@Test
		@DisplayName("성공")
		void success() throws Exception {
			mockMvc.perform(get("/api/admin/members/dashboard/stats").header("Authorization", "Bearer " + adminToken)
					.contentType(MediaType.APPLICATION_JSON))
					.andDo(print())
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value(200))
					.andExpect(jsonPath("$.data.totalMembers").isNumber())
					.andExpect(jsonPath("$.data.activeMembers").isNumber())
					.andExpect(jsonPath("$.data.withdrawnMembers").isNumber())
					.andExpect(jsonPath("$.data.adminCount").isNumber())
					.andExpect(jsonPath("$.data.todaySignups").isNumber())
					.andExpect(jsonPath("$.data.todayLogins").isNumber())
					.andExpect(jsonPath("$.data.todayLoginFailures").isNumber())
					.andExpect(jsonPath("$.data.currentLockedAccounts").isNumber());
		}

		@Test
		@DisplayName("실패 - 일반 회원 권한")
		void fail_withMemberRole() throws Exception {
			mockMvc.perform(get("/api/admin/members/dashboard/stats").header("Authorization", "Bearer " + memberToken)
					.contentType(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isForbidden());
		}
	}
}
