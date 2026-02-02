package com.back.b2st.domain.reservation.dto.response;

import java.util.List;

import com.back.b2st.domain.payment.dto.response.PaymentConfirmRes;

public record ReservationDetailWithPaymentRes(
	ReservationDetailRes reservation,
	List<ReservationSeatInfo> seats,
	PaymentConfirmRes payment
) {
}