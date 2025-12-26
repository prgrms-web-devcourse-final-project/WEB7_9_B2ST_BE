package com.back.b2st.domain.payment.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
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
class PaymentConfirmTransactionService {

	private final PaymentRepository paymentRepository;
	private final Clock clock;

	@Transactional
	public void completeIdempotently(String orderId) {
		try {
			int updated = paymentRepository.completeIfReady(orderId, LocalDateTime.now(clock));
			if (updated == 1) {
				return;
			}
		} catch (DataIntegrityViolationException e) {
			throw new BusinessException(PaymentErrorCode.IDEMPOTENCY_CONFLICT,
				"중복 요청 처리 중 충돌이 발생했습니다.");
		}

		Payment current = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.NOT_FOUND));

		if (current.getStatus() == PaymentStatus.DONE) {
			return;
		}

		throw new BusinessException(PaymentErrorCode.INVALID_STATUS, "결제 상태 변경에 실패했습니다.");
	}
}
