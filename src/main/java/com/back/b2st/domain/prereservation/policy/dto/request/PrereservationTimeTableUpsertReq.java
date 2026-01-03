package com.back.b2st.domain.prereservation.policy.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

public record PrereservationTimeTableUpsertReq(
	@NotNull Long sectionId,
	@NotNull LocalDateTime bookingStartAt,
	@NotNull LocalDateTime bookingEndAt
) {
}

