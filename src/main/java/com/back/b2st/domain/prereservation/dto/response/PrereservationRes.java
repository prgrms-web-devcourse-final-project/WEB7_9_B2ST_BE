package com.back.b2st.domain.prereservation.dto.response;

import java.util.List;

public record PrereservationRes(
	Long scheduleId,
	List<Long> sectionIds
) {
	public static PrereservationRes of(Long scheduleId, List<Long> sectionIds) {
		return new PrereservationRes(scheduleId, sectionIds);
	}
}
