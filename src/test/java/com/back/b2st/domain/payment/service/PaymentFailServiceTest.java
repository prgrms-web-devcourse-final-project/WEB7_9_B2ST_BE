package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class PaymentFailServiceTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private PaymentFailureHandler failureHandler;

	@Test
	void fail_marksPaymentFailed_andCallsReservationFail() {
		String orderId = "ORDER-1";
		Long memberId = 1L;
		Long reservationId = 10L;
		Long amount = 15000L;
		String reason = "user canceled";

		Payment payment = Payment.builder()
			.orderId(orderId)
			.memberId(memberId)
			.domainType(DomainType.RESERVATION)
			.domainId(reservationId)
			.amount(amount)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();

		PaymentFailService paymentFailService = new PaymentFailService(paymentRepository, List.of(failureHandler));
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
		when(failureHandler.supports(DomainType.RESERVATION)).thenReturn(true);

		Payment failed = paymentFailService.fail(memberId, orderId, reason);

		assertThat(failed.getStatus()).isEqualTo(PaymentStatus.FAILED);
		assertThat(failed.getFailureReason()).isEqualTo(reason);
		verify(failureHandler).handleFailure(payment);
	}

	@Test
	void fail_isIdempotent_whenAlreadyFailed_stillCallsReservationFail() {
		String orderId = "ORDER-2";
		Long memberId = 1L;
		Long reservationId = 11L;

		Payment payment = Payment.builder()
			.orderId(orderId)
			.memberId(memberId)
			.domainType(DomainType.RESERVATION)
			.domainId(reservationId)
			.amount(15000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.fail("first fail");

		PaymentFailService paymentFailService = new PaymentFailService(paymentRepository, List.of(failureHandler));
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
		when(failureHandler.supports(DomainType.RESERVATION)).thenReturn(true);

		Payment result = paymentFailService.fail(memberId, orderId, "second fail");

		assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
		assertThat(result.getFailureReason()).isEqualTo("first fail");
		verify(failureHandler).handleFailure(payment);
	}

	@Test
	void fail_throwsInvalidStatus_whenPaymentNotReady() {
		String orderId = "ORDER-3";
		Long memberId = 1L;

		Payment payment = Payment.builder()
			.orderId(orderId)
			.memberId(memberId)
			.domainType(DomainType.RESERVATION)
			.domainId(12L)
			.amount(15000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete(LocalDateTime.now());

		PaymentFailService paymentFailService = new PaymentFailService(paymentRepository, List.of(failureHandler));
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

		assertThatThrownBy(() -> paymentFailService.fail(memberId, orderId, "any"))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.INVALID_STATUS);
	}

	@Test
	void fail_throwsUnauthorized_whenMemberDoesNotOwnPayment() {
		String orderId = "ORDER-4";
		Long ownerId = 1L;
		Long requesterId = 2L;

		Payment payment = Payment.builder()
			.orderId(orderId)
			.memberId(ownerId)
			.domainType(DomainType.RESERVATION)
			.domainId(13L)
			.amount(15000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();

		PaymentFailService paymentFailService = new PaymentFailService(paymentRepository, List.of(failureHandler));
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

		assertThatThrownBy(() -> paymentFailService.fail(requesterId, orderId, "any"))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
	}

	@Test
	void fail_throwsNotFound_whenPaymentDoesNotExist() {
		String orderId = "NON-EXISTENT-ORDER";
		Long memberId = 1L;

		PaymentFailService paymentFailService = new PaymentFailService(paymentRepository, List.of(failureHandler));
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentFailService.fail(memberId, orderId, "any"))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.NOT_FOUND);
	}

	@Test
	void fail_doesNotCallDomainService_whenDomainTypeIsNotReservation() {
		String orderId = "ORDER-5";
		Long memberId = 1L;
		String reason = "user canceled";

		Payment payment = Payment.builder()
			.orderId(orderId)
			.memberId(memberId)
			.domainType(DomainType.TRADE)
			.domainId(999L)
			.amount(15000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();

		PaymentFailService paymentFailService = new PaymentFailService(paymentRepository, List.of(failureHandler));
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
		when(failureHandler.supports(DomainType.TRADE)).thenReturn(false);

		Payment failed = paymentFailService.fail(memberId, orderId, reason);

		assertThat(failed.getStatus()).isEqualTo(PaymentStatus.FAILED);
		assertThat(failed.getFailureReason()).isEqualTo(reason);
		verify(failureHandler, never()).handleFailure(payment);
	}
}
