package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.payment.dto.request.PaymentPrepareReq;
import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentMethod;
import com.back.b2st.domain.payment.entity.PaymentStatus;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.payment.repository.PaymentRepository;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class PaymentPrepareServiceTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private PaymentDomainHandler handler;

	@InjectMocks
	private PaymentPrepareService paymentPrepareService;

	@Test
	void prepare_createsReadyPayment_forCard() {
		paymentPrepareService = new PaymentPrepareService(paymentRepository, List.of(handler), Clock.systemDefaultZone());
		when(handler.supports(DomainType.RESERVATION)).thenReturn(true);
		when(handler.loadAndValidate(10L, 1L))
			.thenReturn(new PaymentTarget(DomainType.RESERVATION, 10L, 15000L));
		when(paymentRepository.findTopByDomainTypeAndDomainIdOrderByCreatedAtDesc(DomainType.RESERVATION, 10L))
			.thenReturn(Optional.empty());
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 1L));

		Payment res = paymentPrepareService.prepare(
			1L,
			new PaymentPrepareReq(DomainType.RESERVATION, 10L, PaymentMethod.CARD)
		);

		assertThat(res.getId()).isEqualTo(1L);
		assertThat(res.getAmount()).isEqualTo(15000L);
		assertThat(res.getStatus()).isEqualTo(PaymentStatus.READY);
		assertThat(res.getExpiresAt()).isNull();
		assertThat(res.getOrderId()).isNotBlank();
	}

	@Test
	void prepare_blocksWhenExistingPaymentIsDone() {
		paymentPrepareService = new PaymentPrepareService(paymentRepository, List.of(handler), Clock.systemDefaultZone());
		when(handler.supports(DomainType.RESERVATION)).thenReturn(true);
		when(handler.loadAndValidate(10L, 1L))
			.thenReturn(new PaymentTarget(DomainType.RESERVATION, 10L, 15000L));
		when(paymentRepository.findTopByDomainTypeAndDomainIdOrderByCreatedAtDesc(DomainType.RESERVATION, 10L))
			.thenReturn(Optional.of(withStatusDone()));

		assertThatThrownBy(() -> paymentPrepareService.prepare(
			1L,
			new PaymentPrepareReq(DomainType.RESERVATION, 10L, PaymentMethod.CARD)
		))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException) ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DUPLICATE_PAYMENT);
	}

	@Test
	void prepare_createsWaitingForDeposit_forVirtualAccount() {
		paymentPrepareService = new PaymentPrepareService(paymentRepository, List.of(handler), Clock.systemDefaultZone());
		when(handler.supports(DomainType.RESERVATION)).thenReturn(true);
		when(handler.loadAndValidate(10L, 1L))
			.thenReturn(new PaymentTarget(DomainType.RESERVATION, 10L, 15000L));
		when(paymentRepository.findTopByDomainTypeAndDomainIdOrderByCreatedAtDesc(DomainType.RESERVATION, 10L))
			.thenReturn(Optional.empty());
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 2L));

		Payment res = paymentPrepareService.prepare(
			1L,
			new PaymentPrepareReq(DomainType.RESERVATION, 10L, PaymentMethod.VIRTUAL_ACCOUNT)
		);

		assertThat(res.getId()).isEqualTo(2L);
		assertThat(res.getAmount()).isEqualTo(15000L);
		assertThat(res.getStatus()).isEqualTo(PaymentStatus.WAITING_FOR_DEPOSIT);
		assertThat(res.getExpiresAt()).isNotNull().isAfter(LocalDateTime.now().minusSeconds(1));
	}

	@Test
	void prepare_blocksWhenExistingPaymentIsReady() {
		paymentPrepareService = new PaymentPrepareService(paymentRepository, List.of(handler), Clock.systemDefaultZone());
		when(handler.supports(DomainType.RESERVATION)).thenReturn(true);
		when(handler.loadAndValidate(10L, 1L))
			.thenReturn(new PaymentTarget(DomainType.RESERVATION, 10L, 15000L));
		when(paymentRepository.findTopByDomainTypeAndDomainIdOrderByCreatedAtDesc(DomainType.RESERVATION, 10L))
			.thenReturn(Optional.of(withStatusReady()));

		assertThatThrownBy(() -> paymentPrepareService.prepare(
			1L,
			new PaymentPrepareReq(DomainType.RESERVATION, 10L, PaymentMethod.CARD)
		))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException) ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DUPLICATE_PAYMENT);
	}

	@Test
	void prepare_blocksWhenExistingPaymentIsWaitingForDeposit() {
		paymentPrepareService = new PaymentPrepareService(paymentRepository, List.of(handler), Clock.systemDefaultZone());
		when(handler.supports(DomainType.RESERVATION)).thenReturn(true);
		when(handler.loadAndValidate(10L, 1L))
			.thenReturn(new PaymentTarget(DomainType.RESERVATION, 10L, 15000L));
		when(paymentRepository.findTopByDomainTypeAndDomainIdOrderByCreatedAtDesc(DomainType.RESERVATION, 10L))
			.thenReturn(Optional.of(withStatusWaitingForDeposit()));

		assertThatThrownBy(() -> paymentPrepareService.prepare(
			1L,
			new PaymentPrepareReq(DomainType.RESERVATION, 10L, PaymentMethod.CARD)
		))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException) ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DUPLICATE_PAYMENT);
	}

	@Test
	void prepare_allowsWhenExistingPaymentIsFailed() {
		paymentPrepareService = new PaymentPrepareService(paymentRepository, List.of(handler), Clock.systemDefaultZone());
		when(handler.supports(DomainType.RESERVATION)).thenReturn(true);
		when(handler.loadAndValidate(10L, 1L))
			.thenReturn(new PaymentTarget(DomainType.RESERVATION, 10L, 15000L));
		when(paymentRepository.findTopByDomainTypeAndDomainIdOrderByCreatedAtDesc(DomainType.RESERVATION, 10L))
			.thenReturn(Optional.of(withStatusFailed()));
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 3L));

		Payment res = paymentPrepareService.prepare(
			1L,
			new PaymentPrepareReq(DomainType.RESERVATION, 10L, PaymentMethod.CARD)
		);

		assertThat(res.getId()).isEqualTo(3L);
	}

	@Test
	void prepare_throwsWhenHandlerNotFound() {
		paymentPrepareService = new PaymentPrepareService(paymentRepository, List.of(handler), Clock.systemDefaultZone());
		when(handler.supports(DomainType.RESERVATION)).thenReturn(false);

		assertThatThrownBy(() -> paymentPrepareService.prepare(
			1L,
			new PaymentPrepareReq(DomainType.RESERVATION, 10L, PaymentMethod.CARD)
		))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException) ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_FOUND);
	}

	private static Payment withStatusDone() {
		Payment payment = Payment.builder()
			.orderId("order-1")
			.memberId(1L)
			.domainType(DomainType.RESERVATION)
			.domainId(10L)
			.amount(15000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		setField(payment, "status", PaymentStatus.DONE);
		return payment;
	}

	private static Payment withStatusReady() {
		Payment payment = Payment.builder()
			.orderId("order-2")
			.memberId(1L)
			.domainType(DomainType.RESERVATION)
			.domainId(10L)
			.amount(15000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		setField(payment, "status", PaymentStatus.READY);
		return payment;
	}

	private static Payment withStatusWaitingForDeposit() {
		Payment payment = Payment.builder()
			.orderId("order-3")
			.memberId(1L)
			.domainType(DomainType.RESERVATION)
			.domainId(10L)
			.amount(15000L)
			.method(PaymentMethod.VIRTUAL_ACCOUNT)
			.expiresAt(LocalDateTime.now().plusDays(1))
			.build();
		setField(payment, "status", PaymentStatus.WAITING_FOR_DEPOSIT);
		return payment;
	}

	private static Payment withStatusFailed() {
		Payment payment = Payment.builder()
			.orderId("order-4")
			.memberId(1L)
			.domainType(DomainType.RESERVATION)
			.domainId(10L)
			.amount(15000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		setField(payment, "status", PaymentStatus.FAILED);
		return payment;
	}

	private static Payment withId(Payment payment, long id) {
		setField(payment, "id", id);
		return payment;
	}

	private static void setField(Object target, String fieldName, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}
}

