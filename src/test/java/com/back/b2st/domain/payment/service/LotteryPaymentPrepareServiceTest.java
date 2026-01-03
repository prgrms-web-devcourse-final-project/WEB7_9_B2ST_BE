package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.lottery.result.dto.LotteryPaymentInfo;
import com.back.b2st.domain.lottery.result.repository.LotteryResultRepository;
import com.back.b2st.domain.payment.dto.request.PaymentPrepareReq;
import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentMethod;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.seat.grade.entity.SeatGradeType;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class LotteryPaymentPrepareServiceTest {

	@Mock
	private LotteryResultRepository lotteryResultRepository;

	@Mock
	private PaymentPrepareService paymentPrepareService;

	@InjectMocks
	private LotteryPaymentPrepareService lotteryPaymentPrepareService;

	@Test
	void prepareByEntryUuid_delegatesToPaymentPrepareService() {
		Long memberId = 1L;
		UUID entryUuid = UUID.randomUUID();
		Long lotteryResultId = 10L;

		when(lotteryResultRepository.findPaymentInfoByid(entryUuid))
			.thenReturn(new LotteryPaymentInfo(lotteryResultId, memberId, SeatGradeType.VIP, 2));

		Payment expected = Payment.builder()
			.orderId("order-1")
			.memberId(memberId)
			.domainType(DomainType.LOTTERY)
			.domainId(lotteryResultId)
			.amount(15000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		when(paymentPrepareService.prepare(any(), any())).thenReturn(expected);

		Payment res = lotteryPaymentPrepareService.prepareByEntryUuid(memberId, entryUuid, PaymentMethod.CARD);

		assertThat(res).isSameAs(expected);

		ArgumentCaptor<PaymentPrepareReq> captor = ArgumentCaptor.forClass(PaymentPrepareReq.class);
		verify(paymentPrepareService).prepare(org.mockito.ArgumentMatchers.eq(memberId), captor.capture());
		PaymentPrepareReq forwarded = captor.getValue();
		assertThat(forwarded.domainType()).isEqualTo(DomainType.LOTTERY);
		assertThat(forwarded.domainId()).isEqualTo(lotteryResultId);
		assertThat(forwarded.paymentMethod()).isEqualTo(PaymentMethod.CARD);
	}

	@Test
	void prepareByEntryUuid_throwsWhenInfoNotFound() {
		UUID entryUuid = UUID.randomUUID();
		when(lotteryResultRepository.findPaymentInfoByid(entryUuid)).thenReturn(null);

		assertThatThrownBy(() -> lotteryPaymentPrepareService.prepareByEntryUuid(1L, entryUuid, PaymentMethod.CARD))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_FOUND);
	}

	@Test
	void prepareByEntryUuid_throwsWhenNotOwner() {
		UUID entryUuid = UUID.randomUUID();
		when(lotteryResultRepository.findPaymentInfoByid(entryUuid))
			.thenReturn(new LotteryPaymentInfo(10L, 999L, SeatGradeType.VIP, 2));

		assertThatThrownBy(() -> lotteryPaymentPrepareService.prepareByEntryUuid(1L, entryUuid, PaymentMethod.CARD))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
	}
}

