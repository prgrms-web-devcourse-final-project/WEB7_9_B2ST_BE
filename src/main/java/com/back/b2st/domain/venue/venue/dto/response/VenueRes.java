package com.back.b2st.domain.venue.venue.dto.response;

import com.back.b2st.domain.venue.venue.entity.Venue;

public record VenueRes(
		Long venueId,
		String name
) {
	public static VenueRes from(Venue venue) {
		return new VenueRes(
				venue.getVenueId(),
				venue.getName()
		);
	}
}
