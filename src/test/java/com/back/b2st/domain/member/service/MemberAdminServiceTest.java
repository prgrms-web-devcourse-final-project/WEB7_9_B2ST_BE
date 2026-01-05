package com.back.b2st.domain.member.service;

import static com.back.b2st.domain.member.fixture.MemberTestFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.back.b2st.domain.auth.repository.LoginLogRepository;
import com.back.b2st.domain.auth.service.AuthAdminService;
import com.back.b2st.domain.member.dto.response.DashboardStatsRes;
import com.back.b2st.domain.member.dto.response.MemberDetailAdminRes;
import com.back.b2st.domain.member.dto.response.MemberSummaryAdminRes;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.error.MemberErrorCode;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class MemberAdminServiceTest {

	@InjectMocks
	private MemberAdminService memberAdminService;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private LoginLogRepository loginLogRepository;

	@Mock
	private AuthAdminService authAdminService;

	@Nested
	@DisplayName("회원 목록 조회")
	class GetMembersTest {

		@Test
		@DisplayName("성공 - 검색 조건 없이 전체 조회")
		void success_noFilter() {
			// given
			Pageable pageable = PageRequest.of(0, 20);
			Member member = createMemberWithId(1L, "test@test.com", "encodedPw");
			Page<Member> memberPage = new PageImpl<>(List.of(member), pageable, 1);

			given(memberRepository.searchMembers(null, null, null, null, pageable))
				.willReturn(memberPage);

			// when
			Page<MemberSummaryAdminRes> result = memberAdminService.getMembers(null, null, null, null, pageable);

			// then
			assertThat(result.getContent()).hasSize(1);
			assertThat(result.getContent().get(0).email()).isEqualTo("test@test.com");
		}

		@Test
		@DisplayName("성공 - 이메일 검색")
		void success_searchByEmail() {
			// given
			Pageable pageable = PageRequest.of(0, 20);
			Member member = createMemberWithId(1L, "admin@test.com", "encodedPw");
			Page<Member> memberPage = new PageImpl<>(List.of(member), pageable, 1);

			given(memberRepository.searchMembers(eq("admin"), isNull(), isNull(), isNull(), eq(pageable)))
				.willReturn(memberPage);

			// when
			Page<MemberSummaryAdminRes> result = memberAdminService.getMembers("admin", null, null, null, pageable);

			// then
			assertThat(result.getContent()).hasSize(1);
			assertThat(result.getContent().get(0).email()).contains("admin");
		}

		@Test
		@DisplayName("성공 - Role 필터")
		void success_filterByRole() {
			// given
			Pageable pageable = PageRequest.of(0, 20);
			Member admin = createAdminMemberWithId(1L, "admin@test.com", "encodedPw");
			Page<Member> memberPage = new PageImpl<>(List.of(admin), pageable, 1);

			given(memberRepository.searchMembers(isNull(), isNull(), eq(Member.Role.ADMIN), isNull(), eq(pageable)))
				.willReturn(memberPage);

			// when
			Page<MemberSummaryAdminRes> result = memberAdminService.getMembers(null, null, Member.Role.ADMIN, null,
				pageable);

			// then
			assertThat(result.getContent()).hasSize(1);
			assertThat(result.getContent().get(0).role()).isEqualTo(Member.Role.ADMIN);
		}

		@Test
		@DisplayName("성공 - 빈 결과")
		void success_emptyResult() {
			// given
			Pageable pageable = PageRequest.of(0, 20);
			Page<Member> emptyPage = new PageImpl<>(List.of(), pageable, 0);

			given(memberRepository.searchMembers(any(), any(), any(), any(), any()))
				.willReturn(emptyPage);

			// when
			Page<MemberSummaryAdminRes> result = memberAdminService.getMembers("nonexistent", null, null, null,
				pageable);

			// then
			assertThat(result.getContent()).isEmpty();
			assertThat(result.getTotalElements()).isZero();
		}
	}

	@Nested
	@DisplayName("회원 상세 조회")
	class GetMemberDetailTest {

		@Test
		@DisplayName("성공")
		void success() {
			// given
			Member member = createMemberWithId(1L, "test@test.com", "encodedPw");
			given(memberRepository.findById(1L)).willReturn(Optional.of(member));

			// when
			MemberDetailAdminRes result = memberAdminService.getMemberDetail(1L);

			// then
			assertThat(result.id()).isEqualTo(1L);
			assertThat(result.email()).isEqualTo("test@test.com");
		}

		@Test
		@DisplayName("실패 - 존재하지 않는 회원")
		void fail_memberNotFound() {
			// given
			given(memberRepository.findById(999L)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> memberAdminService.getMemberDetail(999L))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("대시보드 통계 조회")
	class GetDashboardStatsTest {

		@Test
		@DisplayName("성공")
		void success() {
			// given
			given(memberRepository.count()).willReturn(100L);
			given(memberRepository.countByDeletedAtIsNull()).willReturn(95L);
			given(memberRepository.countByDeletedAtIsNotNull()).willReturn(5L);
			given(memberRepository.countByRole(Member.Role.ADMIN)).willReturn(3L);
			given(memberRepository.countByCreatedAtAfter(any(LocalDateTime.class))).willReturn(10L);
			given(loginLogRepository.countByAttemptedAtAfter(any(LocalDateTime.class))).willReturn(50L);
			given(loginLogRepository.countFailuresByAttemptedAtAfter(any(LocalDateTime.class))).willReturn(5L);
			given(authAdminService.getLockedAccountCount()).willReturn(2);

			// when
			DashboardStatsRes result = memberAdminService.getDashboardStats();

			// then
			assertThat(result.totalMembers()).isEqualTo(100L);
			assertThat(result.activeMembers()).isEqualTo(95L);
			assertThat(result.withdrawnMembers()).isEqualTo(5L);
			assertThat(result.adminCount()).isEqualTo(3L);
			assertThat(result.todaySignups()).isEqualTo(10L);
			assertThat(result.todayLogins()).isEqualTo(50L);
			assertThat(result.todayLoginFailures()).isEqualTo(5L);
			assertThat(result.currentLockedAccounts()).isEqualTo(2);
		}

		@Test
		@DisplayName("성공 - 모든 값이 0인 경우")
		void success_allZero() {
			// given
			given(memberRepository.count()).willReturn(0L);
			given(memberRepository.countByDeletedAtIsNull()).willReturn(0L);
			given(memberRepository.countByDeletedAtIsNotNull()).willReturn(0L);
			given(memberRepository.countByRole(Member.Role.ADMIN)).willReturn(0L);
			given(memberRepository.countByCreatedAtAfter(any(LocalDateTime.class))).willReturn(0L);
			given(loginLogRepository.countByAttemptedAtAfter(any(LocalDateTime.class))).willReturn(0L);
			given(loginLogRepository.countFailuresByAttemptedAtAfter(any(LocalDateTime.class))).willReturn(0L);
			given(authAdminService.getLockedAccountCount()).willReturn(0);

			// when
			DashboardStatsRes result = memberAdminService.getDashboardStats();

			// then
			assertThat(result.totalMembers()).isZero();
			assertThat(result.activeMembers()).isZero();
			assertThat(result.currentLockedAccounts()).isZero();
		}
	}
}
