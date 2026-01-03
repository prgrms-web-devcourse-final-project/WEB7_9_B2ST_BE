package com.back.b2st.domain.prereservation.entry.dto.response;

import java.time.LocalDateTime;

public record PrereservationSectionRes(
	Long sectionId,
	String sectionName,
	LocalDateTime bookingStartAt,
	LocalDateTime bookingEndAt,
	boolean applied
) {
}
