package com.back.b2st.domain.reservation.api;

import java.util.List;

import com.back.b2st.domain.reservation.dto.request.ReservationReq;
import com.back.b2st.domain.reservation.dto.response.ReservationRes;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "예매", description = "예매 생성 및 조회 API")
public interface ReservationApi {

	@Operation(
		summary = "예매 생성",
		description = """
			좌석을 예매합니다.
			- 로그인 사용자만 가능
			- 좌석 상태가 AVAILABLE일 경우 HOLD 처리 후 예매 생성
			"""
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "예매 생성 성공"),
		@ApiResponse(responseCode = "409", description = "이미 선택되었거나 판매된 좌석"),
		@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	BaseResponse<ReservationRes> createReservation(
		@Parameter(hidden = true) UserPrincipal user,
		ReservationReq request
	);

	@Operation(
		summary = "예매 단건 조회",
		description = "예매 ID로 예매 상세 정보를 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "404", description = "예매 정보 없음")
	})
	BaseResponse<ReservationRes> getReservation(
		@Parameter(description = "예매 ID", example = "1")
		Long reservationId
	);

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
}
