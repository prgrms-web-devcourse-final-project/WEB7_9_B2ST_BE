package com.back.b2st.domain.prereservation.policy.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record PrereservationTimeTableUpsertListReq(
	@NotEmpty @Valid List<PrereservationTimeTableUpsertReq> items
) {
}

