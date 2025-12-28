package com.back.b2st.domain.seatapplication.dto.response;

import java.util.List;

public record SeatSectionApplicationRes(
	Long scheduleId,
	List<Long> sectionIds
) {
	public static SeatSectionApplicationRes of(Long scheduleId, List<Long> sectionIds) {
		return new SeatSectionApplicationRes(scheduleId, sectionIds);
	}
}
