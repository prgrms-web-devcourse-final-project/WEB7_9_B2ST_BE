package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.payment.dto.request.PaymentConfirmReq;
import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentMethod;
import com.back.b2st.domain.payment.entity.PaymentStatus;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.payment.repository.PaymentRepository;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class PaymentConfirmServiceTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private PaymentConfirmTransactionService paymentConfirmTransactionService;

	@Mock
	private PaymentFinalizeService paymentFinalizeService;

	@InjectMocks
	private PaymentConfirmService paymentConfirmService;

	@Test
	void confirm_throwsNotFound_whenPaymentDoesNotExist() {
		String orderId = "non-existent-order";
		Long amount = 10000L;
		Long memberId = 1L;

		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentConfirmService.confirm(memberId, new PaymentConfirmReq(orderId, amount)))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.NOT_FOUND);
	}

	@Test
	void confirm_throwsUnauthorized_whenMemberDoesNotOwnPayment() {
		String orderId = "order-123";
		Long amount = 10000L;
		Long ownerId = 1L;
		Long requesterId = 2L;

		Payment payment = Payment.builder()
			.orderId(orderId)
			.memberId(ownerId)
			.domainType(DomainType.RESERVATION)
			.domainId(10L)
			.amount(amount)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();

		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

		assertThatThrownBy(() -> paymentConfirmService.confirm(requesterId, new PaymentConfirmReq(orderId, amount)))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);

		verify(paymentConfirmTransactionService, never()).completeIdempotently(any());
		verify(paymentFinalizeService, never()).finalizeByOrderId(any());
	}

	@Test
	void confirm_returnsPayment_whenAlreadyDone() {
		String orderId = "order-done";
		Long amount = 10000L;
		Long memberId = 1L;

		Payment payment = Payment.builder()
			.orderId(orderId)
			.memberId(memberId)
			.domainType(DomainType.RESERVATION)
			.domainId(10L)
			.amount(amount)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete(java.time.LocalDateTime.now());

		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

		Payment result = paymentConfirmService.confirm(memberId, new PaymentConfirmReq(orderId, amount));

		assertThat(result.getStatus()).isEqualTo(PaymentStatus.DONE);
		verify(paymentConfirmTransactionService, never()).completeIdempotently(any());
		verify(paymentFinalizeService).finalizeByOrderId(orderId);
	}

	@Test
	void confirm_throwsInvalidStatus_whenNotReady() {
		String orderId = "order-failed";
		Long amount = 10000L;
		Long memberId = 1L;

		Payment payment = Payment.builder()
			.orderId(orderId)
			.memberId(memberId)
			.domainType(DomainType.RESERVATION)
			.domainId(10L)
			.amount(amount)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.fail("Test failure");

		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

		assertThatThrownBy(() -> paymentConfirmService.confirm(memberId, new PaymentConfirmReq(orderId, amount)))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.INVALID_STATUS);

		verify(paymentConfirmTransactionService, never()).completeIdempotently(any());
		verify(paymentFinalizeService, never()).finalizeByOrderId(any());
	}

	@Test
	void confirm_throwsNotFound_whenPaymentDisappearsAfterTransaction() {
		String orderId = "order-disappeared";
		Long amount = 10000L;
		Long memberId = 1L;

		Payment payment = Payment.builder()
			.orderId(orderId)
			.memberId(memberId)
			.domainType(DomainType.RESERVATION)
			.domainId(10L)
			.amount(amount)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();

		when(paymentRepository.findByOrderId(orderId))
			.thenReturn(Optional.of(payment))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentConfirmService.confirm(memberId, new PaymentConfirmReq(orderId, amount)))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.NOT_FOUND);
	}
}
