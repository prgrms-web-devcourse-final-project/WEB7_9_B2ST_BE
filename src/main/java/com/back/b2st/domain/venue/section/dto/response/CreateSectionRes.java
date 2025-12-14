package com.back.b2st.domain.venue.section.dto.response;

import com.back.b2st.domain.venue.section.entity.Section;

public record CreateSectionRes(
	Long sectionId,
	Long venueId,
	String sectionName
) {
	public CreateSectionRes(Section section) {
		this(
			section.getId(),
			section.getVenueId(),
			section.getSectionName()
		);
	}
}
