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

@Tag(
	name = "좌석",
	description = "회차별 좌석 조회 및 좌석 HOLD API"
)
@RequestMapping("/api/schedules")
public interface ScheduleSeatApi {

	/* ==================================================
	 * 회차별 좌석 조회 (전체 / 상태별)
	 * ================================================== */

	@Operation(
		summary = "회차별 좌석 조회 (전체/상태별)",
		description = """
			특정 회차의 좌석 목록을 조회합니다.
			- status 미지정: 전체 좌석 조회 (ScheduleSeatService.getSeats)
			- status 지정: 해당 상태 좌석만 조회 (ScheduleSeatService.getSeatsByStatus)
			"""
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "좌석 조회 성공"),
		@ApiResponse(responseCode = "404", description = "회차 정보 없음 (SCHEDULE_NOT_FOUND)")
	})
	@GetMapping("/{scheduleId}/seats")
	BaseResponse<List<ScheduleSeatViewRes>> getScheduleSeats(
		@Parameter(description = "공연 회차 ID", example = "1")
		@PathVariable Long scheduleId,

		@Parameter(
			description = "좌석 상태 (AVAILABLE / HOLD / SOLD)",
			example = "AVAILABLE",
			required = false
		)
		@RequestParam(required = false) SeatStatus status
	);

	/* ==================================================
	 * 티켓팅 화면 전용 (AVAILABLE만)
	 * ================================================== */

	@Operation(
		summary = "티켓팅 전용 좌석 조회 (AVAILABLE)",
		description = """
			티켓팅 화면에서 사용되는 API입니다.
			- AVAILABLE 상태 좌석만 조회합니다.
			- 내부적으로 getSeatsByStatus(scheduleId, AVAILABLE) 호출
			"""
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "좌석 조회 성공"),
		@ApiResponse(responseCode = "404", description = "회차 정보 없음 (SCHEDULE_NOT_FOUND)")
	})
	@GetMapping("/ticketing/schedules/{scheduleId}/seats")
	BaseResponse<List<ScheduleSeatViewRes>> getAvailableSeatsForTicketing(
		@Parameter(description = "공연 회차 ID", example = "1")
		@PathVariable Long scheduleId
	);

	/* ==================================================
	 * 좌석 HOLD (AVAILABLE → HOLD)
	 * ================================================== */

	@Operation(
		summary = "좌석 HOLD",
		description = """
			좌석을 HOLD 상태로 변경합니다.
			- 로그인 사용자만 가능
			- Redis 분산 락(3초 TTL) 획득 후 처리
			- DB 좌석 상태: AVAILABLE → HOLD
			- Redis HOLD 소유권 토큰 저장(5분 TTL)
			
			주의)
			- 이 API는 HOLD '소유권 검증'을 수행하지 않습니다.
			  (SEAT_HOLD_EXPIRED / SEAT_HOLD_FORBIDDEN은 이 API에서 발생하지 않음)
			"""
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "좌석 HOLD 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)"),
		@ApiResponse(
			responseCode = "404",
			description = """
				좌석 정보 없음
				- SEAT_NOT_FOUND
				"""
		),
		@ApiResponse(
			responseCode = "409",
			description = """
				좌석 상태 충돌 / 락 획득 실패
				- SEAT_ALREADY_HOLD (이미 HOLD 상태)
				- SEAT_ALREADY_SOLD (이미 SOLD 상태)
				- SEAT_LOCK_FAILED (락 획득 실패)
				"""
		)
	})
	@SecurityRequirement(name = "Authorization")
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