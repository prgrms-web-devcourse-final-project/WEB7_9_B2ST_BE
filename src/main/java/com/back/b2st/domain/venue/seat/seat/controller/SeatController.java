package com.back.b2st.domain.venue.seat.seat.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.venue.constants.SeatApiPath;
import com.back.b2st.domain.venue.seat.seat.dto.request.CreateSeatReq;
import com.back.b2st.domain.venue.seat.seat.dto.response.CreateSeatRes;
import com.back.b2st.domain.venue.seat.seat.service.SeatService;
import com.back.b2st.global.common.BaseResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SeatController {

	private final SeatService seatService;

	@PostMapping(SeatApiPath.CREATE)
	public BaseResponse<CreateSeatRes> createSeat(
		@PathVariable("venueId") Long venueId,
		@Valid @RequestBody CreateSeatReq request
	) {
		return BaseResponse.created(seatService.createSeatInfo(venueId, request));
	}
}
