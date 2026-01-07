package com.back.b2st.domain.venue.section.dto.response;

import com.back.b2st.domain.venue.section.entity.Section;

public record CreateSectionRes(
	Long sectionId,
	Long venueId,
	String sectionName
) {
	public static CreateSectionRes form(Section section) {
		return new CreateSectionRes(
			section.getId(),
			section.getVenueId(),
			section.getSectionName()
		);
	}
}
