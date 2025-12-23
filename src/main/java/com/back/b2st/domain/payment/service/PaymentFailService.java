package com.back.b2st.domain.payment.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentStatus;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.payment.repository.PaymentRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentFailService {

	private final PaymentRepository paymentRepository;
	private final List<PaymentFailureHandler> failureHandlers;

	@Transactional
	public Payment fail(Long memberId, String orderId, String reason) {
		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.NOT_FOUND));

		payment.validateOwner(memberId);

		// 멱등: 이미 실패 처리된 경우에도 도메인 후처리를 재시도할 수 있도록 허용
		if (payment.getStatus() == PaymentStatus.FAILED) {
			handleDomainFailure(payment);
			return payment;
		}

		// READY에서만 FAILED로 전이 가능 (Payment.validateTransition 내에서도 방어)
		if (payment.getStatus() != PaymentStatus.READY) {
			throw new BusinessException(PaymentErrorCode.INVALID_STATUS, "결제 실패 처리가 불가능한 상태입니다.");
		}

		payment.fail(reason);
		handleDomainFailure(payment);

		return payment;
	}

	private void handleDomainFailure(Payment payment) {
		failureHandlers.stream()
			.filter(h -> h.supports(payment.getDomainType()))
			.findFirst()
			.ifPresent(h -> h.handleFailure(payment));
	}
}
