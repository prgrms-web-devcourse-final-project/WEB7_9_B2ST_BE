package com.back.b2st.domain.venue.venue.controller;

import com.back.b2st.domain.venue.venue.dto.response.VenueRes;
import com.back.b2st.domain.venue.venue.service.VenueService;
import com.back.b2st.global.common.BaseResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/venues")
public class VenueController {

	private final VenueService venueService;

	@GetMapping("/{venueId}")
	public BaseResponse<VenueRes> getVenue(@PathVariable Long venueId) {
		return BaseResponse.success(venueService.getVenue(venueId));
	}
}

