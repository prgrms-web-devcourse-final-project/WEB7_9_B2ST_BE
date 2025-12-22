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
import com.back.b2st.domain.reservation.dto.response.ReservationCreateRes;
import com.back.b2st.domain.reservation.dto.response.ReservationDetailRes;
import com.back.b2st.domain.reservation.service.ReservationService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
public class ReservationController implements ReservationApi {

	private final ReservationService reservationService;

	/** === 예매 생성 === */
	@PostMapping
	public BaseResponse<ReservationCreateRes> createReservation(
		@CurrentUser UserPrincipal user,
		@RequestBody ReservationReq request
	) {
		return BaseResponse.created(
			reservationService.createReservation(user.getId(), request)
		);
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

	/** === 예매 조회 === */
	@GetMapping("/{reservationId}")
	public BaseResponse<ReservationDetailRes> getReservationDetail(
		@PathVariable Long reservationId,
		@CurrentUser UserPrincipal user
	) {
		return BaseResponse.success(
			reservationService.getReservationDetail(reservationId, user.getId())
		);
	}

	/** === 전체 예매 조회 === */
	@GetMapping("/me")
	public BaseResponse<List<ReservationDetailRes>> getMyReservationsDetail(
		@CurrentUser UserPrincipal user
	) {
		Long memberId = user.getId();
		List<ReservationDetailRes> reservations = reservationService.getMyReservationsDetail(memberId);
		return BaseResponse.success(reservations);
	}
}
