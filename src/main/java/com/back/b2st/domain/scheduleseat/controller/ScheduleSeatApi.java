package com.back.b2st.domain.scheduleseat.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.back.b2st.domain.scheduleseat.dto.response.ScheduleSeatViewRes;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "좌석", description = "회차 좌석 조회 및 좌석 HOLD")
@RequestMapping("/api/schedules")
public interface ScheduleSeatApi {

	@Operation(
		summary = "회차 좌석 조회",
		description = "특정 회차의 좌석 목록을 조회합니다. status가 없으면 전체, 있으면 해당 상태만 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "좌석 조회 성공"),
		@ApiResponse(responseCode = "404", description = "회차 정보 없음 (SCHEDULE_NOT_FOUND)")
	})
	@GetMapping("/{scheduleId}/seats")
	BaseResponse<List<ScheduleSeatViewRes>> getScheduleSeats(
		@Parameter(description = "공연 회차 ID", example = "1")
		@PathVariable Long scheduleId,

		@Parameter(description = "좌석 상태 (AVAILABLE / HOLD / SOLD)", example = "AVAILABLE")
		@RequestParam(required = false) SeatStatus status
	);

	@Operation(
		summary = "좌석 HOLD",
		description = "좌석을 HOLD 상태로 변경합니다. (AVAILABLE → HOLD)"
	)
	@SecurityRequirement(name = "Authorization")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "좌석 HOLD 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)"),
		@ApiResponse(responseCode = "404", description = "좌석 정보 없음 (SEAT_NOT_FOUND)"),
		@ApiResponse(
			responseCode = "409",
			description = "상태 충돌/락 실패 (SEAT_ALREADY_HOLD / SEAT_ALREADY_SOLD / SEAT_LOCK_FAILED)"
		)
	})
	@PostMapping("/{scheduleId}/seats/{seatId}/hold")
	BaseResponse<Void> holdSeat(
		@Parameter(hidden = true)
		@CurrentUser UserPrincipal user,

		@Parameter(description = "공연 회차 ID", example = "1")
		@PathVariable Long scheduleId,

		@Parameter(description = "좌석 ID", example = "101")
		@PathVariable Long seatId
	);
}
