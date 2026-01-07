package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.payment.repository.PaymentRepository;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class PaymentConfirmTransactionServiceTest {

	@Mock
	private PaymentRepository paymentRepository;

	private PaymentConfirmTransactionService service;

	@BeforeEach
	void setup() {
		service = new PaymentConfirmTransactionService(paymentRepository, Clock.systemDefaultZone());
	}

	@Test
	void completeIdempotently_throwsNotFound_whenPaymentNotFoundAfterUpdate() {
		String orderId = "non-existent-order";

		when(paymentRepository.completeIfReady(anyString(), any())).thenReturn(0);
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.completeIdempotently(orderId))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.NOT_FOUND);
	}

	@Test
	void completeIdempotently_throwsIdempotencyConflict_onDataIntegrityViolation() {
		String orderId = "order-conflict";

		when(paymentRepository.completeIfReady(anyString(), any()))
			.thenThrow(new org.springframework.dao.DataIntegrityViolationException("Constraint violation"));

		assertThatThrownBy(() -> service.completeIdempotently(orderId))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.IDEMPOTENCY_CONFLICT);
	}
}
