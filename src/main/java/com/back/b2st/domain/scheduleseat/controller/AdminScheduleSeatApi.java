package com.back.b2st.domain.scheduleseat.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.back.b2st.domain.scheduleseat.dto.response.ScheduleSeatViewRes;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "회차별 좌석 (관리자)", description = "관리자용 회차 좌석 조회 및 HOLD 해제")
@RequestMapping("/api/admin/schedules/{scheduleId}/seats")
@SecurityRequirement(name = "Authorization")
public interface AdminScheduleSeatApi {

	@Operation(
		summary = "회차 좌석 조회 (관리자)",
		description = "특정 회차의 좌석 목록을 조회합니다. status가 없으면 전체, 있으면 해당 상태만 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "좌석 조회 성공"),
		@ApiResponse(responseCode = "404", description = "회차 정보 없음 (SCHEDULE_NOT_FOUND)")
	})
	@GetMapping
	BaseResponse<List<ScheduleSeatViewRes>> getScheduleSeats(
		@Parameter(description = "공연 회차 ID", example = "1")
		@PathVariable Long scheduleId,

		@Parameter(description = "좌석 상태 (AVAILABLE / HOLD / SOLD)", example = "HOLD")
		@RequestParam(required = false) SeatStatus status
	);

	@Operation(
		summary = "좌석 HOLD 강제 해제 (관리자)",
		description = "HOLD 상태의 좌석을 AVAILABLE 상태로 강제 복구합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "HOLD 해제 성공 (응답 바디 없음)"),
		@ApiResponse(responseCode = "404", description = "좌석 정보 없음 (SEAT_NOT_FOUND)")
	})
	@PostMapping("/{seatId}/release-hold")
	void releaseHold(
		@Parameter(description = "공연 회차 ID", example = "1")
		@PathVariable Long scheduleId,

		@Parameter(description = "좌석 ID", example = "101")
		@PathVariable Long seatId
	);

}