package com.back.b2st.domain.payment.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.payment.dto.request.PaymentPayReq;
import com.back.b2st.domain.payment.dto.request.PaymentPrepareReq;
import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.payment.repository.PaymentRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentOneClickService {

	private final PaymentPrepareService paymentPrepareService;
	private final LotteryPaymentPrepareService lotteryPaymentPrepareService;
	private final PaymentConfirmTransactionService paymentConfirmTransactionService;
	private final PaymentFinalizeService paymentFinalizeService;
	private final PaymentRepository paymentRepository;

	@Transactional
	public Payment pay(Long memberId, PaymentPayReq request) {
		if (request.domainType() == DomainType.LOTTERY) {
			if (request.entryId() == null) {
				throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND, "추첨 응모 ID(entryId)가 필요합니다.");
			}
			Payment payment = lotteryPaymentPrepareService.prepareByEntryUuid(
				memberId,
				request.entryId(),
				request.paymentMethod()
			);
			return completeAndFinalize(payment.getOrderId());
		}

		if (request.domainId() == null) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND, "결제 대상 ID(domainId)가 필요합니다.");
		}

		Payment payment = paymentPrepareService.prepare(
			memberId,
			new PaymentPrepareReq(request.domainType(), request.domainId(), request.paymentMethod())
		);
		return completeAndFinalize(payment.getOrderId());
	}

	private Payment completeAndFinalize(String orderId) {
		paymentConfirmTransactionService.completeIdempotently(orderId);
		paymentFinalizeService.finalizeByOrderId(orderId);
		return paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.NOT_FOUND));
	}
}
