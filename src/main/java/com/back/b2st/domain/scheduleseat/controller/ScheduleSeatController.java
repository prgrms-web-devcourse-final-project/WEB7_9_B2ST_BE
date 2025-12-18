package com.back.b2st.domain.scheduleseat.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.scheduleseat.dto.response.ScheduleSeatViewRes;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.service.ScheduleSeatService;
import com.back.b2st.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "좌석", description = "회차별 좌석 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
public class ScheduleSeatController {

	private final ScheduleSeatService scheduleSeatService;

	/** === 회차별 좌석 전체 / 상태별 조회 === */
	@Operation(
		summary = "회차별 좌석 조회",
		description = """
			특정 회차의 좌석 목록을 조회합니다.
			- status 미지정 시 전체 조회
			- status 지정 시 해당 상태 좌석만 조회
			"""
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "404", description = "회차 정보 없음")
	})
	@GetMapping("/{scheduleId}/seats")
	public BaseResponse<List<ScheduleSeatViewRes>> getScheduleSeats(
		@Parameter(description = "공연 회차 ID", example = "1")
		@PathVariable Long scheduleId,

		@Parameter(
			description = "좌석 상태 (AVAILABLE / HOLD / SOLD)",
			example = "AVAILABLE",
			required = false
		)
		@RequestParam(required = false) SeatStatus status
	) {
		if (status == null) {
			return BaseResponse.success(scheduleSeatService.getSeats(scheduleId));
		}
		return BaseResponse.success(scheduleSeatService.getSeatsByStatus(scheduleId, status));
	}

	/** === 티켓팅 화면용 AVAILABLE 좌석 조회 === */
	@Operation(
		summary = "티켓팅 전용 좌석 조회",
		description = "티켓팅 화면에서 사용되는 AVAILABLE 상태 좌석 조회 API"
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "404", description = "회차 정보 없음")
	})
	@GetMapping("/ticketing/schedules/{scheduleId}/seats")
	public BaseResponse<List<ScheduleSeatViewRes>> getAvailableSeatsForTicketing(
		@Parameter(description = "공연 회차 ID", example = "1")
		@PathVariable Long scheduleId
	) {
		return BaseResponse.success(
			scheduleSeatService.getSeatsByStatus(scheduleId, SeatStatus.AVAILABLE)
		);
	}
}
