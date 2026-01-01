package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.prereservation.booking.service.PrereservationBookingService;

@ExtendWith(MockitoExtension.class)
class PrereservationPaymentFailureHandlerTest {

	@Mock
	private PrereservationBookingService prereservationBookingService;

	@InjectMocks
	private PrereservationPaymentFailureHandler prereservationPaymentFailureHandler;

	private static final Long BOOKING_ID = 1L;

	@Test
	@DisplayName("supports(): DomainType.PRERESERVATION 지원")
	void supports_prereservation_true() {
		// when & then
		assertThat(prereservationPaymentFailureHandler.supports(DomainType.PRERESERVATION)).isTrue();
	}

	@Test
	@DisplayName("supports(): 다른 도메인 타입은 미지원")
	void supports_others_false() {
		// when & then
		assertThat(prereservationPaymentFailureHandler.supports(DomainType.RESERVATION)).isFalse();
		assertThat(prereservationPaymentFailureHandler.supports(DomainType.LOTTERY)).isFalse();
		assertThat(prereservationPaymentFailureHandler.supports(DomainType.TRADE)).isFalse();
	}

	@Test
	@DisplayName("handleFailure(): 결제 실패 시 예약 실패 처리")
	void handleFailure_success() {
		// given
		Payment payment = mock(Payment.class);
		given(payment.getDomainId()).willReturn(BOOKING_ID);

		willDoNothing().given(prereservationBookingService).failBooking(BOOKING_ID);

		// when
		assertThatCode(() -> prereservationPaymentFailureHandler.handleFailure(payment))
			.doesNotThrowAnyException();

		// then
		then(prereservationBookingService).should().failBooking(BOOKING_ID);
	}
}
