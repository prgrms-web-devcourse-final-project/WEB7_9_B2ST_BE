package com.back.b2st.domain.member.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.auth.repository.LoginLogRepository;
import com.back.b2st.domain.auth.service.AuthAdminService;
import com.back.b2st.domain.member.dto.response.DashboardStatsRes;
import com.back.b2st.domain.member.dto.response.MemberDetailAdminRes;
import com.back.b2st.domain.member.dto.response.MemberSummaryAdminRes;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.error.MemberErrorCode;
import com.back.b2st.domain.member.repository.MemberRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MemberAdminService {
	private final MemberRepository memberRepository;
	private final LoginLogRepository loginLogRepository;
	private final AuthAdminService authAdminService;

	/**
	 * 회원 목록 조회
	 */
	public Page<MemberSummaryAdminRes> getMembers(
		String email, String name, Member.Role role, Boolean isDeleted, Pageable pageable
	) {
		return memberRepository.searchMembers(email, name, role, isDeleted, pageable)
			.map(MemberSummaryAdminRes::from);
	}

	/**
	 * 회원 상세 조회
	 */
	public MemberDetailAdminRes getMemberDetail(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
		return MemberDetailAdminRes.from(member);
	}

	/**
	 * 대시보드 통계 조회
	 */
	public DashboardStatsRes getDashboardStats() {
		LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
		return new DashboardStatsRes(
			memberRepository.count(), // 전체 회원 수
			memberRepository.countByDeletedAtIsNull(), // 활성 회원 수
			memberRepository.countByDeletedAtIsNotNull(), // 탈퇴 회원 수
			memberRepository.countByRole(Member.Role.ADMIN), // 관리자 수
			memberRepository.countByCreatedAtAfter(todayStart), // 오늘 가입한 회원 수
			loginLogRepository.countByAttemptedAtAfter(todayStart), // 오늘 로그인 시도 수
			loginLogRepository.countFailuresByAttemptedAtAfter(todayStart), // 오늘 로그인 실패 수
			authAdminService.getLockedAccountCount() // 잠긴 계정 수
		);
	}
}
