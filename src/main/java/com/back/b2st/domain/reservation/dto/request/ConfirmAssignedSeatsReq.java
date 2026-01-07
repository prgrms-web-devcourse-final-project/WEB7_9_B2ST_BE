package com.back.b2st.domain.reservation.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ConfirmAssignedSeatsReq(
	@NotNull Long scheduleId,
	@NotEmpty List<Long> scheduleSeatIds
) {
}