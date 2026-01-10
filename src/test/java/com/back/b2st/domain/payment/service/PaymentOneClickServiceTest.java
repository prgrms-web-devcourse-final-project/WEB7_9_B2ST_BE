package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.payment.dto.request.PaymentPayReq;
import com.back.b2st.domain.payment.dto.request.PaymentPrepareReq;
import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentMethod;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.payment.repository.PaymentRepository;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class PaymentOneClickServiceTest {

	@Mock
	private PaymentPrepareService paymentPrepareService;

	@Mock
	private LotteryPaymentPrepareService lotteryPaymentPrepareService;

	@Mock
	private PaymentConfirmTransactionService paymentConfirmTransactionService;

	@Mock
	private PaymentFinalizeService paymentFinalizeService;

	@Mock
	private PaymentRepository paymentRepository;

	@InjectMocks
	private PaymentOneClickService paymentOneClickService;

	@Test
	void pay_runsPrepareThenCompleteAndFinalize() {
		Long memberId = 1L;
		PaymentPayReq request = new PaymentPayReq(DomainType.RESERVATION, PaymentMethod.CARD, 10L, null);

		Payment prepared = Payment.builder()
			.orderId("order-1")
			.memberId(memberId)
			.domainType(DomainType.RESERVATION)
			.domainId(10L)
			.amount(15000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();

		when(paymentPrepareService.prepare(memberId, new PaymentPrepareReq(DomainType.RESERVATION, 10L, PaymentMethod.CARD)))
			.thenReturn(prepared);
		when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(prepared));

		Payment res = paymentOneClickService.pay(memberId, request);

		assertThat(res).isSameAs(prepared);
		verify(paymentConfirmTransactionService).completeIdempotently("order-1");
		verify(paymentFinalizeService).finalizeByOrderId("order-1");
	}

	@Test
	void pay_lottery_delegatesToLotteryPrepareThenCompletes() {
		Long memberId = 1L;
		UUID entryId = UUID.randomUUID();
		PaymentPayReq request = new PaymentPayReq(DomainType.LOTTERY, PaymentMethod.CARD, null, entryId);

		Payment prepared = Payment.builder()
			.orderId("order-2")
			.memberId(memberId)
			.domainType(DomainType.LOTTERY)
			.domainId(99L)
			.amount(30000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();

		when(lotteryPaymentPrepareService.prepareByEntryUuid(memberId, entryId, PaymentMethod.CARD)).thenReturn(prepared);
		when(paymentRepository.findByOrderId("order-2")).thenReturn(Optional.of(prepared));

		Payment res = paymentOneClickService.pay(memberId, request);

		assertThat(res).isSameAs(prepared);

		ArgumentCaptor<UUID> entryCaptor = ArgumentCaptor.forClass(UUID.class);
		verify(lotteryPaymentPrepareService).prepareByEntryUuid(
			org.mockito.ArgumentMatchers.eq(memberId),
			entryCaptor.capture(),
			org.mockito.ArgumentMatchers.eq(PaymentMethod.CARD)
		);
		assertThat(entryCaptor.getValue()).isEqualTo(entryId);
		verify(paymentConfirmTransactionService).completeIdempotently("order-2");
		verify(paymentFinalizeService).finalizeByOrderId("order-2");
	}

	@Test
	void pay_throwsWhenDomainIdMissing_forNonLottery() {
		PaymentPayReq request = new PaymentPayReq(DomainType.RESERVATION, PaymentMethod.CARD, null, null);

		assertThatThrownBy(() -> paymentOneClickService.pay(1L, request))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_FOUND);
	}

	@Test
	void pay_throwsWhenEntryIdMissing_forLottery() {
		PaymentPayReq request = new PaymentPayReq(DomainType.LOTTERY, PaymentMethod.CARD, null, null);

		assertThatThrownBy(() -> paymentOneClickService.pay(1L, request))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_FOUND);
	}

	@Test
	void pay_throwsWhenPaymentNotFound_afterFinalize() {
		Long memberId = 1L;
		PaymentPayReq request = new PaymentPayReq(DomainType.RESERVATION, PaymentMethod.CARD, 10L, null);

		Payment prepared = Payment.builder()
			.orderId("order-3")
			.memberId(memberId)
			.domainType(DomainType.RESERVATION)
			.domainId(10L)
			.amount(15000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();

		when(paymentPrepareService.prepare(memberId, new PaymentPrepareReq(DomainType.RESERVATION, 10L, PaymentMethod.CARD)))
			.thenReturn(prepared);
		when(paymentRepository.findByOrderId("order-3")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentOneClickService.pay(memberId, request))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.NOT_FOUND);
		verify(paymentConfirmTransactionService).completeIdempotently("order-3");
		verify(paymentFinalizeService).finalizeByOrderId("order-3");
	}
}
