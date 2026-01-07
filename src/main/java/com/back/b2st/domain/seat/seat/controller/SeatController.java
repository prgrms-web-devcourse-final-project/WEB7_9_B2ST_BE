package com.back.b2st.domain.seat.seat.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.seat.seat.dto.request.CreateSeatReq;
import com.back.b2st.domain.seat.seat.dto.response.CreateSeatRes;
import com.back.b2st.domain.seat.seat.service.SeatService;
import com.back.b2st.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Seat", description = "좌석 API")
public class SeatController {

	private final SeatService seatService;

	@Operation(summary = "좌석 생성", description = "공연장의 좌석을 등록")
	@PostMapping("/admin/venues/{venueId}/seats")
	public BaseResponse<CreateSeatRes> createSeat(
		@Parameter(description = "공연장 ID", example = "1")
		@PathVariable("venueId") Long venueId,
		@Valid @RequestBody CreateSeatReq request
	) {
		return BaseResponse.created(seatService.createSeatInfo(venueId, request));
	}
}
