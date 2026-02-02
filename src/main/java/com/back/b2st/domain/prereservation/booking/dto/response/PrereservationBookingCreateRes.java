package com.back.b2st.domain.prereservation.booking.dto.response;

import java.time.LocalDateTime;

public record PrereservationBookingCreateRes(
	Long prereservationBookingId,
	LocalDateTime expiresAt
) {
}

