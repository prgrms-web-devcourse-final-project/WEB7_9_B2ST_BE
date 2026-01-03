package com.back.b2st.domain.reservation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record SeatReservationResult(
	List<Long> scheduleSeatIds,
	LocalDateTime expiresAt
) {
}