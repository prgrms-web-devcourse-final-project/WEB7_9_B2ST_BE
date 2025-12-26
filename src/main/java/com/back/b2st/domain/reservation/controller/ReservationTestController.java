package com.back.b2st.domain.reservation.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.reservation.service.ReservationService;
import com.back.b2st.global.common.BaseResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/reservations")
public class ReservationTestController {

	private final ReservationService reservationService;

	/** === 예매 확정(결제 성공 시나리오 테스트) === */
	@PostMapping("/{reservationId}/complete")
	public BaseResponse<Void> completeReservation(
		@PathVariable Long reservationId
	) {
		reservationService.completeReservation(reservationId);
		return BaseResponse.created(null);
	}

	/** === 예매 실패(결제 실패 시나리오 테스트) === */
	@PostMapping("/{reservationId}/fail")
	public BaseResponse<Void> failReservation(
		@PathVariable Long reservationId
	) {
		reservationService.failReservation(reservationId);
		return BaseResponse.created(null);
	}

	/** === 예매 만료(예매 만료 시나리오 테스트): PENDING -> EXPIRED + 좌석 AVAILABLE 복구 === */
	@PostMapping("/{reservationId}/expire")
	public BaseResponse<Void> expireReservation(
		@PathVariable Long reservationId
	) {
		reservationService.expireReservation(reservationId);
		return BaseResponse.created(null);
	}
}
