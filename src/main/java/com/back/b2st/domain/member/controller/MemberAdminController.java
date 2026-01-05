package com.back.b2st.domain.member.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.member.dto.response.DashboardStatsRes;
import com.back.b2st.domain.member.dto.response.MemberDetailAdminRes;
import com.back.b2st.domain.member.dto.response.MemberSummaryAdminRes;
import com.back.b2st.domain.member.entity.Member;
import com.back.b2st.domain.member.service.MemberAdminService;
import com.back.b2st.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/members")
@Tag(name = "MemberAdminController", description = "회원 관리 API (관리자 전용)")
@SecurityRequirement(name = "BearerAuth")
public class MemberAdminController {

	private final MemberAdminService memberAdminService;

	/**
	 * 회원 목록 조회 - 검색 + 필터링 + 페이징
	 */
	@GetMapping
	@Operation(summary = "회원 목록 조회", description = "검색 조건 설정하여 회원 목록 조회")
	public BaseResponse<Page<MemberSummaryAdminRes>> getMembers(
		@Parameter(description = "이메일 검색") @RequestParam(required = false) String email,
		@Parameter(description = "이름 검색") @RequestParam(required = false) String name,
		@Parameter(description = "권한 필터") @RequestParam(required = false) Member.Role role,
		@Parameter(description = "탈퇴 여부") @RequestParam(required = false) Boolean isDeleted,
		@Parameter(hidden = true) @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		Page<MemberSummaryAdminRes> response = memberAdminService.getMembers(email, name, role, isDeleted, pageable);
		return BaseResponse.success(response);
	}

	/**
	 * 회원 상세 조회
	 */
	@GetMapping("/{memberId}")
	@Operation(summary = "회원 상세 조회")
	public BaseResponse<MemberDetailAdminRes> getMemberDetail(
		@Parameter(description = "회원 ID", example = "1") @PathVariable Long memberId
	) {
		return BaseResponse.success(memberAdminService.getMemberDetail(memberId));
	}

	/**
	 * 대시보드 통계 조회
	 */
	@GetMapping("/dashboard/stats")
	@Operation(summary = "대시보드 통계", description = "회원/로그인/보안 통계 데이터")
	public BaseResponse<DashboardStatsRes> getDashboardStats() {
		return BaseResponse.success(memberAdminService.getDashboardStats());
	}
}
