package com.back.b2st.domain.payment.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.lottery.entry.entity.LotteryEntry;
import com.back.b2st.domain.lottery.entry.repository.LotteryEntryRepository;
import com.back.b2st.domain.lottery.result.entity.LotteryResult;
import com.back.b2st.domain.lottery.result.repository.LotteryResultRepository;
import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.seat.grade.entity.SeatGrade;
import com.back.b2st.domain.seat.grade.repository.SeatGradeRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LotteryPaymentHandler implements PaymentDomainHandler {

	private final LotteryResultRepository lotteryResultRepository;
	private final LotteryEntryRepository lotteryEntryRepository;
	private final SeatGradeRepository seatGradeRepository;
	private final Clock clock;

	@Override
	public boolean supports(DomainType domainType) {
		return domainType == DomainType.LOTTERY;
	}

	@Override
	@Transactional(readOnly = true)
	public PaymentTarget loadAndValidate(Long lotteryResultId, Long memberId) {
		LotteryResult lotteryResult = lotteryResultRepository.findById(lotteryResultId)
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND));

		if (!lotteryResult.getMemberId().equals(memberId)) {
			throw new BusinessException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
		}

		if (lotteryResult.isPaid()) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
		}

		LocalDateTime now = LocalDateTime.now(clock);
		if (now.isAfter(lotteryResult.getPaymentDeadline())) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE, "결제 가능 시간이 만료되었습니다.");
		}

		LotteryEntry lotteryEntry = lotteryEntryRepository.findById(lotteryResult.getLotteryEntryId())
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND));

		SeatGrade seatGrade = seatGradeRepository.findTopByPerformanceIdAndGradeOrderByIdDesc(
				lotteryEntry.getPerformanceId(),
				lotteryEntry.getGrade()
			)
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND));

		Long expectedAmount = seatGrade.getPrice().longValue() * lotteryEntry.getQuantity();
		return new PaymentTarget(DomainType.LOTTERY, lotteryResultId, expectedAmount);
	}
}

