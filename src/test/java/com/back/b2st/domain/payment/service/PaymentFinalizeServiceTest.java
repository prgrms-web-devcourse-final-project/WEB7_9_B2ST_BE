package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentMethod;
import com.back.b2st.domain.payment.entity.PaymentStatus;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.payment.repository.PaymentRepository;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class PaymentFinalizeServiceTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private PaymentFinalizer finalizer;

	@InjectMocks
	private PaymentFinalizeService paymentFinalizeService;

	@Test
	void finalizeByOrderId_callsFinalizer_whenPaymentIsDone() {
		String orderId = "order-123";
		Payment payment = createDonePayment(orderId);

		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
		when(finalizer.supports(DomainType.RESERVATION)).thenReturn(true);

		paymentFinalizeService = new PaymentFinalizeService(paymentRepository, List.of(finalizer));
		paymentFinalizeService.finalizeByOrderId(orderId);

		verify(finalizer).finalizePayment(payment);
	}

	@Test
	void finalizeByOrderId_throwsNotFound_whenPaymentDoesNotExist() {
		String orderId = "non-existent";

		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

		paymentFinalizeService = new PaymentFinalizeService(paymentRepository, List.of(finalizer));

		assertThatThrownBy(() -> paymentFinalizeService.finalizeByOrderId(orderId))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.NOT_FOUND);

		verify(finalizer, never()).finalizePayment(any());
	}

	@Test
	void finalizeByOrderId_throwsInvalidStatus_whenPaymentIsNotDone() {
		String orderId = "order-pending";
		Payment payment = Payment.builder()
			.orderId(orderId)
			.memberId(1L)
			.domainType(DomainType.RESERVATION)
			.domainId(10L)
			.amount(15000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();

		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

		paymentFinalizeService = new PaymentFinalizeService(paymentRepository, List.of(finalizer));

		assertThatThrownBy(() -> paymentFinalizeService.finalizeByOrderId(orderId))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.INVALID_STATUS);

		verify(finalizer, never()).finalizePayment(any());
	}

	@Test
	void finalizeByOrderId_throwsDomainNotFound_whenNoFinalizerSupports() {
		String orderId = "order-123";
		Payment payment = createDonePayment(orderId);

		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
		when(finalizer.supports(DomainType.RESERVATION)).thenReturn(false);

		paymentFinalizeService = new PaymentFinalizeService(paymentRepository, List.of(finalizer));

		assertThatThrownBy(() -> paymentFinalizeService.finalizeByOrderId(orderId))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_FOUND);

		verify(finalizer, never()).finalizePayment(any());
	}

	private Payment createDonePayment(String orderId) {
		Payment payment = Payment.builder()
			.orderId(orderId)
			.memberId(1L)
			.domainType(DomainType.RESERVATION)
			.domainId(10L)
			.amount(15000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete(LocalDateTime.now());
		return payment;
	}
}
