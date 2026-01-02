package com.back.b2st.domain.payment.service;

import org.springframework.stereotype.Component;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.prereservation.booking.service.PrereservationBookingService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PrereservationPaymentFailureHandler implements PaymentFailureHandler {

	private final PrereservationBookingService prereservationBookingService;

	@Override
	public boolean supports(DomainType domainType) {
		return domainType == DomainType.PRERESERVATION;
	}

	@Override
	public void handleFailure(Payment payment) {
		prereservationBookingService.failBooking(payment.getDomainId());
	}
}
