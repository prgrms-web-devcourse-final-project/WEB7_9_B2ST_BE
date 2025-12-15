package com.back.b2st.domain.reservation.api;

import java.util.List;

import com.back.b2st.domain.reservation.dto.request.ReservationReq;
import com.back.b2st.domain.reservation.dto.response.ReservationRes;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "예매", description = "예매 생성 / 조회 / 취소 / 확정 API")
public interface ReservationApi {

	/* =======================
	 *   1. 예매 생성
	 * ======================= */
	@Operation(
		summary = "예매 생성",
		description = """
			좌석을 예매합니다.
			- 로그인 사용자만 가능
			- 좌석 상태가 AVAILABLE일 때만 HOLD 후 예매 생성
			- Redis 분산락 기반 동시성 처리
			"""
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "예매 생성 성공"),
		@ApiResponse(responseCode = "409", description = "이미 HOLD 또는 SOLD된 좌석"),
		@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	BaseResponse<ReservationRes> createReservation(
		@Parameter(hidden = true) UserPrincipal user,

		@RequestBody(
			description = "예매 요청 정보 (performanceId, seatId)"
		) ReservationReq request
	);

	/* =======================
	 *   2. 예매 단건 조회
	 * ======================= */
	@Operation(
		summary = "예매 상세 조회",
		description = """
			예매 ID로 예매 상세 정보를 조회합니다.
			- 로그인 사용자 본인 예약만 조회 가능
			"""
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "404", description = "예매 정보 없음"),
		@ApiResponse(responseCode = "403", description = "본인의 예매가 아님")
	})
	BaseResponse<ReservationRes> getReservation(
		@Parameter(description = "예매 ID", example = "1") Long reservationId,

		@Parameter(hidden = true) UserPrincipal user
	);

	/* =======================
	 *   3. 내 예매 목록 조회
	 * ======================= */
	@Operation(
		summary = "내 예매 목록 조회",
		description = "로그인한 사용자의 모든 예매 내역을 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	BaseResponse<List<ReservationRes>> getMyReservations(
		@Parameter(hidden = true) UserPrincipal user
	);

	/* =======================
	 *   4. 예매 취소
	 * ======================= */
	@Operation(
		summary = "예매 취소",
		description = """
			사용자의 예매를 취소합니다.
			- 이미 결제 완료된 예매는 취소 불가
			- 취소 시 좌석 상태는 AVAILABLE로 복구됨
			"""
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "취소 성공"),
		@ApiResponse(responseCode = "403", description = "본인의 예매가 아님"),
		@ApiResponse(responseCode = "404", description = "예매 정보 없음"),
		@ApiResponse(responseCode = "409", description = "결제 완료된 예매는 취소 불가")
	})
	BaseResponse<Void> cancelReservation(
		@Parameter(description = "예매 ID", example = "1") Long reservationId,

		@Parameter(hidden = true) UserPrincipal user
	);

	/* =======================
	 *   5. 예매 확정 (결제 완료)
	 * ======================= */
	@Operation(
		summary = "예매 확정(결제 완료)",
		description = """
			결제 성공 시 예매를 확정합니다.
			- 좌석은 SOLD 상태로 변경됩니다.
			- 취소된 예매는 확정할 수 없습니다.
			"""
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "확정 성공"),
		@ApiResponse(responseCode = "403", description = "본인의 예매가 아님"),
		@ApiResponse(responseCode = "404", description = "예매 정보 없음"),
		@ApiResponse(responseCode = "409", description = "취소된 예매는 확정 불가")
	})
	BaseResponse<Void> completeReservation(
		@Parameter(description = "예매 ID", example = "1") Long reservationId,

		@Parameter(hidden = true) UserPrincipal user
	);
}
