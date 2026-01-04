package com.back.b2st.domain.prereservation.entry.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.prereservation.entry.dto.response.PrereservationRes;
import com.back.b2st.domain.prereservation.entry.service.PrereservationApplyService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/prereservations/applications")
@Tag(name = "신청 예매", description = "사전 구역 신청 및 신청 내역 조회 API")
public class PrereservationMyApplicationController {

	private final PrereservationApplyService prereservationApplyService;

	@GetMapping("/me")
	@Operation(
		summary = "내 사전 신청 전체 조회",
		description = """
			로그인한 사용자의 전체 회차 사전 신청 구역 목록을 조회합니다.

			# REQUEST
			GET /api/prereservations/applications/me

			# RESPONSE (200 OK)
			{
			  "code": 200,
			  "message": "성공적으로 처리되었습니다",
			  "data": [
			    { "scheduleId": 1, "sectionIds": [1, 3] },
			    { "scheduleId": 2, "sectionIds": [5] }
			  ]
			}
			"""
	)
	@SecurityRequirement(name = "Authorization")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "내 사전 신청 전체 조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)")
	})
	public BaseResponse<List<PrereservationRes>> getMyApplications(
		@Parameter(hidden = true) @CurrentUser UserPrincipal user
	) {
		return BaseResponse.success(prereservationApplyService.getMyApplicationList(user.getId()));
	}
}
