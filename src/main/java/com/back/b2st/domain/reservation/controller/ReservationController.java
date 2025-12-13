package com.back.b2st.domain.reservation.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.reservation.dto.request.ReservationRequest;
import com.back.b2st.domain.reservation.dto.response.ReservationResponse;
import com.back.b2st.domain.reservation.service.ReservationService;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reservations")
public class ReservationController {

	private final ReservationService reservationService;

	/** === 예매 생성 === */
	@PostMapping
	public BaseResponse<ReservationResponse> createReservation(
		@AuthenticationPrincipal CustomUserDetails user,
		@RequestBody ReservationRequest request
	) {
		// TODO: 추후 서비스단으로 로직 이동..?
		Long memberId = user.getId();

		ReservationResponse response = reservationService.createReservation(memberId, request);
		return BaseResponse.success(response);
	}

	/** === 예매 단건 조회 === */
	@GetMapping("/{reservationId}")
	public BaseResponse<ReservationResponse> getReservation(
		@PathVariable Long reservationId
	) {
		// TODO: 추후 구현 예정
		return BaseResponse.success();
	}
}
