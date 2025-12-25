package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.reservation.service.ReservationService;

@ExtendWith(MockitoExtension.class)
class ReservationPaymentFailureHandlerTest {

	@Mock
	private ReservationService reservationService;

	@InjectMocks
	private ReservationPaymentFailureHandler handler;

	@Test
	@DisplayName("supports: RESERVATION 타입을 지원한다")
	void supports_returnsTrue_forReservation() {
		// When & Then
		assertThat(handler.supports(DomainType.RESERVATION)).isTrue();
	}

	@Test
	@DisplayName("supports: TRADE 타입을 지원하지 않는다")
	void supports_returnsFalse_forTrade() {
		// When & Then
		assertThat(handler.supports(DomainType.TRADE)).isFalse();
	}

	@Test
	@DisplayName("handleFailure: 결제 실패 시 reservationService.failReservation()을 호출한다")
	void handleFailure_callsFailReservation() {
		// Given
		Long domainId = 123L;
		Payment payment = mock(Payment.class);
		when(payment.getDomainId()).thenReturn(domainId);

		// When
		handler.handleFailure(payment);

		// Then
		verify(reservationService, times(1)).failReservation(domainId);
	}
}
