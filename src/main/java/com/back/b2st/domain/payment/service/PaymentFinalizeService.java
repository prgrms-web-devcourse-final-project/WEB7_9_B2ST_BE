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
public class PaymentFinalizeService {

	private final PaymentRepository paymentRepository;
	private final List<PaymentFinalizer> finalizers;

	@Transactional
	public void finalizeByOrderId(String orderId) {
		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.NOT_FOUND));

		if (payment.getStatus() != PaymentStatus.DONE) {
			throw new BusinessException(PaymentErrorCode.INVALID_STATUS, "DONE 상태의 결제만 확정 처리를 수행할 수 있습니다.");
		}

		PaymentFinalizer finalizer = finalizers.stream()
			.filter(f -> f.supports(payment.getDomainType()))
			.findFirst()
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND, "결제 확정 처리를 지원하지 않는 도메인입니다."));

		finalizer.finalizePayment(payment);
	}
}

