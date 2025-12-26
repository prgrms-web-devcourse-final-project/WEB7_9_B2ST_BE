package com.back.b2st.domain.reservation.dto.response;

import com.back.b2st.domain.payment.dto.response.PaymentConfirmRes;

public record ReservationDetailWithPaymentRes(
	ReservationDetailRes reservation,
	PaymentConfirmRes payment
) {
}