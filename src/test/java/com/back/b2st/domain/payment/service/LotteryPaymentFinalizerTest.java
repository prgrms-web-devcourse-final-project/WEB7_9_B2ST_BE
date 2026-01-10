package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.back.b2st.domain.lottery.result.entity.LotteryResult;
import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@ExtendWith(MockitoExtension.class)
class LotteryPaymentFinalizerTest {

	@Mock
	private EntityManager entityManager;

	@InjectMocks
	private LotteryPaymentFinalizer lotteryPaymentFinalizer;

	private static final Long LOTTERY_RESULT_ID = 10L;
	private static final Long MEMBER_ID = 1L;

	@Test
	@DisplayName("supports(): DomainType.LOTTERY 지원")
	void supports_lottery_true() {
		assertThat(lotteryPaymentFinalizer.supports(DomainType.LOTTERY)).isTrue();
	}

	@Test
	@DisplayName("supports(): 다른 도메인 타입은 미지원")
	void supports_others_false() {
		assertThat(lotteryPaymentFinalizer.supports(DomainType.RESERVATION)).isFalse();
		assertThat(lotteryPaymentFinalizer.supports(DomainType.PRERESERVATION)).isFalse();
		assertThat(lotteryPaymentFinalizer.supports(DomainType.TRADE)).isFalse();
	}

	@Test
	@DisplayName("finalizePayment(): 추첨 결과가 없으면 DOMAIN_NOT_FOUND 예외")
	void finalizePayment_resultNotFound_throw() {
		Payment payment = org.mockito.Mockito.mock(Payment.class);
		given(payment.getDomainId()).willReturn(LOTTERY_RESULT_ID);

		given(entityManager.find(LotteryResult.class, LOTTERY_RESULT_ID, LockModeType.PESSIMISTIC_WRITE))
			.willReturn(null);

		assertThatThrownBy(() -> lotteryPaymentFinalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_FOUND);
	}

	@Test
	@DisplayName("finalizePayment(): 다른 사용자의 추첨 결과면 UNAUTHORIZED_PAYMENT_ACCESS 예외")
	void finalizePayment_unauthorized_throw() {
		Payment payment = org.mockito.Mockito.mock(Payment.class);
		given(payment.getDomainId()).willReturn(LOTTERY_RESULT_ID);
		given(payment.getMemberId()).willReturn(MEMBER_ID);

		LotteryResult lotteryResult = org.mockito.Mockito.mock(LotteryResult.class);
		given(lotteryResult.getMemberId()).willReturn(999L);
		given(entityManager.find(LotteryResult.class, LOTTERY_RESULT_ID, LockModeType.PESSIMISTIC_WRITE))
			.willReturn(lotteryResult);

		assertThatThrownBy(() -> lotteryPaymentFinalizer.finalizePayment(payment))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException)ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
	}

	@Test
	@DisplayName("finalizePayment(): 미결제인 경우 paid=true 마킹 수행")
	void finalizePayment_marksPaid_whenNotPaid() {
		Payment payment = org.mockito.Mockito.mock(Payment.class);
		given(payment.getDomainId()).willReturn(LOTTERY_RESULT_ID);
		given(payment.getMemberId()).willReturn(MEMBER_ID);

		LotteryResult lotteryResult = org.mockito.Mockito.mock(LotteryResult.class);
		given(lotteryResult.getMemberId()).willReturn(MEMBER_ID);
		given(lotteryResult.isPaid()).willReturn(false);
		given(entityManager.find(LotteryResult.class, LOTTERY_RESULT_ID, LockModeType.PESSIMISTIC_WRITE))
			.willReturn(lotteryResult);

		assertThatCode(() -> lotteryPaymentFinalizer.finalizePayment(payment)).doesNotThrowAnyException();

		then(lotteryResult).should().confirmPayment();
	}

	@Test
	@DisplayName("finalizePayment(): 이미 결제된 경우 멱등 처리")
	void finalizePayment_idempotent_whenAlreadyPaid() {
		Payment payment = org.mockito.Mockito.mock(Payment.class);
		given(payment.getDomainId()).willReturn(LOTTERY_RESULT_ID);
		given(payment.getMemberId()).willReturn(MEMBER_ID);

		LotteryResult lotteryResult = org.mockito.Mockito.mock(LotteryResult.class);
		given(lotteryResult.getMemberId()).willReturn(MEMBER_ID);
		given(lotteryResult.isPaid()).willReturn(true);
		given(entityManager.find(LotteryResult.class, LOTTERY_RESULT_ID, LockModeType.PESSIMISTIC_WRITE))
			.willReturn(lotteryResult);

		assertThatCode(() -> lotteryPaymentFinalizer.finalizePayment(payment)).doesNotThrowAnyException();

		then(lotteryResult).should(org.mockito.Mockito.never()).confirmPayment();
	}
}
