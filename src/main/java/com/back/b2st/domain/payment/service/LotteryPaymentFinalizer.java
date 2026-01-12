package com.back.b2st.domain.payment.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.lottery.entry.entity.LotteryEntry;
import com.back.b2st.domain.lottery.result.entity.LotteryResult;
import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.reservation.service.LotteryReservationService;
import com.back.b2st.global.error.exception.BusinessException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LotteryPaymentFinalizer implements PaymentFinalizer {

	private final LotteryReservationService lotteryReservationService;
	private final EntityManager entityManager;

	@Override
	public boolean supports(DomainType domainType) {
		return domainType == DomainType.LOTTERY;
	}

	@Override
	@Transactional
	public void finalizePayment(Payment payment) {
		LotteryResult lotteryResult = entityManager.find(LotteryResult.class, payment.getDomainId(),
			LockModeType.PESSIMISTIC_WRITE);

		if (lotteryResult == null) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND);
		}

		if (!lotteryResult.getMemberId().equals(payment.getMemberId())) {
			throw new BusinessException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
		}

		// 결제 완료 마킹만 수행 (좌석/티켓 생성은 추후 배치에서 처리)
		if (!lotteryResult.isPaid()) {
			lotteryResult.confirmPayment();
		}

		LotteryEntry lotteryEntry = entityManager.find(LotteryEntry.class, lotteryResult.getLotteryEntryId());
		if (lotteryEntry == null) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND);
		}

		lotteryReservationService.getOrCreateCompletedReservation(payment.getMemberId(), lotteryEntry.getScheduleId());
	}
}
