package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.payment.dto.response.PaymentConfirmRes;
import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentMethod;
import com.back.b2st.domain.payment.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentViewServiceTest {

	@Mock
	private PaymentRepository paymentRepository;

	@InjectMocks
	private PaymentViewService paymentViewService;

	@Test
	@DisplayName("getByReservationId(): domainType=RESERVATION 결제 조회")
	void getByReservationId_success() {
		// given
		Payment payment = Payment.builder()
			.orderId("ORDER-1")
			.memberId(1L)
			.domainType(DomainType.RESERVATION)
			.domainId(10L)
			.amount(1000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();

		when(paymentRepository.findByDomainTypeAndDomainIdAndMemberId(DomainType.RESERVATION, 10L, 1L))
			.thenReturn(Optional.of(payment));

		// when
		PaymentConfirmRes res = paymentViewService.getByReservationId(10L, 1L);

		// then
		assertThat(res).isNotNull();
		assertThat(res.orderId()).isEqualTo("ORDER-1");
	}

	@Test
	@DisplayName("getByDomain(): 결제가 없으면 null 반환")
	void getByDomain_empty_returnsNull() {
		// given
		when(paymentRepository.findByDomainTypeAndDomainIdAndMemberId(DomainType.PRERESERVATION, 1L, 2L))
			.thenReturn(Optional.empty());

		// when
		PaymentConfirmRes res = paymentViewService.getByDomain(DomainType.PRERESERVATION, 1L, 2L);

		// then
		assertThat(res).isNull();
	}
}

