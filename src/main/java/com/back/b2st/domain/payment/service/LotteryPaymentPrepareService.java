package com.back.b2st.domain.payment.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.lottery.result.dto.LotteryPaymentInfo;
import com.back.b2st.domain.lottery.result.repository.LotteryResultRepository;
import com.back.b2st.domain.payment.dto.request.PaymentPrepareReq;
import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentMethod;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LotteryPaymentPrepareService {

	private final LotteryResultRepository lotteryResultRepository;
	private final PaymentPrepareService paymentPrepareService;

	@Transactional
	public Payment prepareByEntryUuid(Long memberId, UUID entryUuid, PaymentMethod paymentMethod) {
		LotteryPaymentInfo info = lotteryResultRepository.findPaymentInfoByid(entryUuid);
		if (info == null) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND);
		}

		if (!info.memberId().equals(memberId)) {
			throw new BusinessException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
		}

		return paymentPrepareService.prepare(
			memberId,
			new PaymentPrepareReq(DomainType.LOTTERY, info.id(), paymentMethod)
		);
	}
}

