package com.back.b2st.domain.reservation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.back.b2st.domain.reservation.dto.request.ReservationReq;
import com.back.b2st.domain.reservation.dto.response.ReservationCreateRes;
import com.back.b2st.domain.reservation.dto.response.ReservationDetailRes;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "예매", description = "예매 생성 / 조회 / 취소 API")
public interface ReservationApi {

	/* =========================
	 * 1. 예매 생성(결제 시작)
	 * ========================= */
	@Operation(
		summary = "예매 생성(결제 시작)",
		description = """
			좌석 HOLD가 완료된 상태에서 예매(PENDING)를 생성합니다.
			- 로그인 사용자만 가능
			- 본인이 HOLD한 좌석만 생성 가능
			- HOLD TTL 만료 시 생성 불가 (410 GONE)
			- 이미 활성 상태(PENDING/COMPLETED) 예매가 존재하면 생성 불가 (409 CONFLICT)
			"""
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "예매(PENDING) 생성 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패"),
		@ApiResponse(responseCode = "403", description = "HOLD 소유자가 아님 (SEAT_HOLD_FORBIDDEN)"),
		@ApiResponse(responseCode = "404", description = "좌석 정보 없음 (SEAT_NOT_FOUND)"),
		@ApiResponse(responseCode = "409", description = """
			상태 충돌
			- RESERVATION_ALREADY_EXISTS (활성 상태 예매 중복)
			- SEAT_NOT_HOLD (좌석이 HOLD 상태가 아님)
			"""),
		@ApiResponse(responseCode = "410", description = "좌석 선점 시간이 만료됨 (SEAT_HOLD_EXPIRED)")
	})
	@PostMapping
	BaseResponse<ReservationCreateRes> createReservation(
		@Parameter(hidden = true)
		UserPrincipal user,
		@RequestBody(description = "예매 요청 정보 (scheduleId, seatId)")
		ReservationReq request
	);

	/* =========================
	 * 2. 예매 취소
	 * ========================= */
	@Operation(
		summary = "예매 취소",
		description = """
			예매를 취소합니다.
			- 본인 예매만 취소 가능
			- 현재 상태에서 취소가 불가능하면 실패합니다. (INVALID_RESERVATION_STATUS)
			- 취소 시 좌석은 HOLD → AVAILABLE로 복구됩니다.
			"""
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "취소 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패"),
		@ApiResponse(responseCode = "403", description = "본인의 예매가 아님 (RESERVATION_FORBIDDEN)"),
		@ApiResponse(responseCode = "404", description = "예매 정보 없음 (RESERVATION_NOT_FOUND)"),
		@ApiResponse(responseCode = "409", description = "현재 예매 상태에서는 요청 불가 (INVALID_RESERVATION_STATUS)")
	})
	@DeleteMapping("/{reservationId}")
	BaseResponse<Void> cancelReservation(
		@Parameter(description = "예매 ID", example = "1")
		@PathVariable Long reservationId,
		@Parameter(hidden = true)
		UserPrincipal user
	);

	/* =========================
	 * 3. 예매 상세 조회
	 * ========================= */
	@Operation(
		summary = "예매 상세 조회",
		description = """
			예매 ID로 예매 상세 정보를 조회합니다.
			- 공연 / 회차 / 좌석 정보 포함
			- 로그인 사용자 본인 예매만 조회 가능(쿼리에서 memberId 조건으로 제한)
			"""
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패"),
		@ApiResponse(responseCode = "404", description = "예매 정보 없음 (RESERVATION_NOT_FOUND)")
	})
	@GetMapping("/{reservationId}")
	BaseResponse<ReservationDetailRes> getReservationDetail(
		@Parameter(description = "예매 ID", example = "1")
		@PathVariable Long reservationId,
		@Parameter(hidden = true)
		UserPrincipal user
	);

	/* =========================
	 * 4. 내 예매 목록 조회(디테일)
	 * ========================= */
	@Operation(
		summary = "내 예매 목록 조회(디테일)",
		description = """
			로그인한 사용자의 모든 예매 내역을 조회합니다.
			- 공연 / 회차 / 좌석 정보 포함
			"""
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	@GetMapping("/me")
	BaseResponse<List<ReservationDetailRes>> getMyReservationsDetail(
		@Parameter(hidden = true)
		UserPrincipal user
	);
}
