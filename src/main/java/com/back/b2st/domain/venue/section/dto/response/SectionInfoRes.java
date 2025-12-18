package com.back.b2st.domain.venue.section.dto.response;

import java.util.List;

import com.back.b2st.domain.venue.section.entity.Section;

public record SectionInfoRes(
	Long venueId,
	List<String> sections
) {
	public static SectionInfoRes from(Long venueId, List<Section> sections) {
		return new SectionInfoRes(
			venueId,
			sections.stream()
				.map(Section::getSectionName)
				.toList()
		);
	}
}
