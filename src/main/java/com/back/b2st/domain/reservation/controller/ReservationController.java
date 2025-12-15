package com.back.b2st.domain.reservation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.reservation.dto.request.ReservationReq;
import com.back.b2st.domain.reservation.dto.response.ReservationRes;
import com.back.b2st.domain.reservation.service.ReservationService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
public class ReservationController {

	private final ReservationService reservationService;

	/** === 예매 생성 === */
	@PostMapping
	public BaseResponse<ReservationRes> createReservation(
		@CurrentUser UserPrincipal user,
		@RequestBody ReservationReq request
	) {
		Long memberId = user.getId();

		ReservationRes response = reservationService.createReservation(memberId, request);
		return BaseResponse.created(response);
	}

	/** === 예매 취소 === */
	@DeleteMapping("/{reservationId}")
	public BaseResponse<Void> cancelReservation(
		@PathVariable Long reservationId,
		@CurrentUser UserPrincipal user
	) {
		Long memberId = user.getId();
		reservationService.cancelReservation(reservationId, memberId);
		return BaseResponse.success();
	}

	/** === 예매 확정(결제 완료) === */
	@PostMapping("/{reservationId}/complete")
	public BaseResponse<Void> completeReservation(
		@PathVariable Long reservationId,
		@CurrentUser UserPrincipal user
	) {
		Long memberId = user.getId();
		reservationService.completeReservation(reservationId, memberId);
		return BaseResponse.success();
	}

	/** === 예매 단건 조회 === */
	@GetMapping("/{reservationId}")
	public BaseResponse<ReservationRes> getReservation(
		@PathVariable Long reservationId,
		@CurrentUser UserPrincipal user
	) {
		Long memberId = user.getId();
		ReservationRes response = reservationService.getReservation(reservationId, memberId);
		return BaseResponse.success(response);
	}

	/** === 전체 예매 조회 === */
	@GetMapping("/me")
	public BaseResponse<List<ReservationRes>> getMyReservations(
		@CurrentUser UserPrincipal user
	) {
		Long memberId = user.getId();
		List<ReservationRes> reservations = reservationService.getMyReservations(memberId);
		return BaseResponse.success(reservations);
	}
}