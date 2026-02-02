package com.back.b2st.domain.reservation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.back.b2st.domain.reservation.dto.request.ReservationReq;
import com.back.b2st.domain.reservation.dto.response.ReservationCreateRes;
import com.back.b2st.domain.reservation.dto.response.ReservationDetailWithPaymentRes;
import com.back.b2st.domain.reservation.dto.response.ReservationRes;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "예매", description = "예매 생성/취소 및 조회")
@RequestMapping("/api/reservations")
@SecurityRequirement(name = "Authorization")
public interface ReservationApi {

	@Operation(summary = "예매 생성", description = "사용자가 예매를 생성합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "예매 생성 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)"),
		@ApiResponse(responseCode = "404", description = "회차/좌석 등 대상 정보 없음"),
		@ApiResponse(responseCode = "409", description = "상태 충돌 (좌석 선점/만료/중복 등)")
	})
	@PostMapping
	BaseResponse<ReservationCreateRes> createReservation(
		@Parameter(hidden = true) @CurrentUser UserPrincipal user,
		@RequestBody ReservationReq request
	);

	@Operation(summary = "예매 취소", description = "사용자가 본인 예매를 취소합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "예매 취소 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)"),
		@ApiResponse(responseCode = "403", description = "권한 없음 (본인 예매 아님)"),
		@ApiResponse(responseCode = "404", description = "예매 정보 없음")
	})
	@DeleteMapping("/{reservationId}")
	BaseResponse<Void> cancelReservation(
		@Parameter(description = "예매 ID", example = "1")
		@PathVariable Long reservationId,
		@Parameter(hidden = true) @CurrentUser UserPrincipal user
	);

	@Operation(summary = "예매 상세 조회", description = "예매 상세 및 결제 정보를 함께 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "예매 상세 조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)"),
		@ApiResponse(responseCode = "403", description = "권한 없음 (본인 예매 아님)"),
		@ApiResponse(responseCode = "404", description = "예매 정보 없음")
	})
	@GetMapping("/{reservationId}")
	BaseResponse<ReservationDetailWithPaymentRes> getReservationDetail(
		@Parameter(description = "예매 ID", example = "1")
		@PathVariable Long reservationId,
		@Parameter(hidden = true) @CurrentUser UserPrincipal user
	);

	@Operation(summary = "내 예매 목록 조회", description = "로그인 사용자의 예매 목록을 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "내 예매 목록 조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)")
	})
	@GetMapping("/me")
	BaseResponse<List<ReservationRes>> getMyReservations(
		@Parameter(hidden = true) @CurrentUser UserPrincipal user
	);
}
