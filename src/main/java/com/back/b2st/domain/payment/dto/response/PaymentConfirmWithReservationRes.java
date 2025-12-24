package com.back.b2st.domain.payment.dto.response;

import com.back.b2st.domain.reservation.dto.response.ReservationDetailRes;

public record PaymentConfirmWithReservationRes(
	ReservationDetailRes reservation,
	PaymentConfirmRes payment
) {
}