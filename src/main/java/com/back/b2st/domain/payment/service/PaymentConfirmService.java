package com.back.b2st.domain.payment.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.payment.dto.request.PaymentConfirmReq;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentStatus;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.payment.repository.PaymentRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentConfirmService {

	private final PaymentRepository paymentRepository;
	private final PaymentConfirmTransactionService paymentConfirmTransactionService;
	private final PaymentFinalizeService paymentFinalizeService;

	@Transactional
	public Payment confirm(Long memberId, PaymentConfirmReq request) {
		Payment payment = paymentRepository.findByOrderId(request.orderId())
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.NOT_FOUND));

		validateOwner(payment, memberId);
		payment.validateAmount(request.amount());

		if (payment.getStatus() == PaymentStatus.DONE) {
			paymentFinalizeService.finalizeByOrderId(request.orderId());
			return payment;
		}

		if (payment.getStatus() != PaymentStatus.READY
			&& payment.getStatus() != PaymentStatus.WAITING_FOR_DEPOSIT) {
			throw new BusinessException(PaymentErrorCode.INVALID_STATUS);
		}

		paymentConfirmTransactionService.completeIdempotently(request.orderId());

		paymentFinalizeService.finalizeByOrderId(request.orderId());

		return paymentRepository.findByOrderId(request.orderId())
			.map(confirmed -> {
				validateOwner(confirmed, memberId);
				return confirmed;
			})
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.NOT_FOUND));
	}

	private void validateOwner(Payment payment, Long memberId) {
		if (!payment.getMemberId().equals(memberId)) {
			throw new BusinessException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
		}
	}
}
