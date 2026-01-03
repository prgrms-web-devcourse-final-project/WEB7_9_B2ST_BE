package com.back.b2st.domain.prereservation.entry.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.prereservation.entry.dto.request.PrereservationReq;
import com.back.b2st.domain.prereservation.entry.dto.response.PrereservationRes;
import com.back.b2st.domain.prereservation.entry.dto.response.PrereservationSectionRes;
import com.back.b2st.domain.prereservation.entry.service.PrereservationApplyService;
import com.back.b2st.domain.prereservation.entry.service.PrereservationSectionService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/prereservations/schedules/{scheduleId}/applications")
@Tag(name = "신청 예매", description = "사전 구역 신청 및 신청 내역 조회 API")
public class PrereservationController {

	private final PrereservationApplyService prereservationApplyService;
	private final PrereservationSectionService prereservationSectionService;

	@PostMapping
	@Operation(
		summary = "사전 구역 신청",
		description = """
			BookingType이 PRERESERVE(신청 예매)인 회차에 대해 예매 가능한 구역을 사전에 신청합니다.
			- 예매 오픈 시간(bookingOpenAt) 24시간 전부터 신청 가능
			- 예매 오픈 시간(bookingOpenAt) 이후 신청 불가
			- 동일 회차/구역 중복 신청 불가
			"""
	)
	public BaseResponse<Void> apply(
		@Parameter(description = "공연 회차 ID", example = "1")
		@PathVariable Long scheduleId,
		@Parameter(hidden = true) @CurrentUser UserPrincipal user,
		@Valid @RequestBody PrereservationReq request
	) {
		prereservationApplyService.apply(scheduleId, user.getId(), user.getEmail(), request.sectionId());
		return BaseResponse.created(null);
	}

	@GetMapping("/sections")
	@Operation(
		summary = "회차별 구역 목록 조회(신청 예매)",
		description = """
			특정 회차의 공연장 구역 목록을 조회합니다.
			- 각 구역별 예매 가능 시간대(고정 슬롯) 포함
			- 로그인 사용자 기준 applied=true/false 포함
			"""
	)
	public BaseResponse<List<PrereservationSectionRes>> getSections(
		@Parameter(description = "공연 회차 ID", example = "1")
		@PathVariable Long scheduleId,
		@Parameter(hidden = true) @CurrentUser UserPrincipal user
	) {
		return BaseResponse.success(prereservationSectionService.getSections(scheduleId, user.getId()));
	}

	@GetMapping("/me")
	@Operation(
		summary = "내 사전 신청 조회",
		description = "로그인한 사용자의 특정 회차 사전 신청 구역 목록을 조회합니다."
	)
	public BaseResponse<PrereservationRes> getMyApplications(
		@Parameter(description = "공연 회차 ID", example = "1")
		@PathVariable Long scheduleId,
		@Parameter(hidden = true) @CurrentUser UserPrincipal user
	) {
		return BaseResponse.success(prereservationApplyService.getMyApplications(scheduleId, user.getId()));
	}
}
