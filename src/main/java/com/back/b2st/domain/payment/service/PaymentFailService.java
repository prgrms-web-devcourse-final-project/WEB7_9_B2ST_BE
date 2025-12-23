package com.back.b2st.domain.payment.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentStatus;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.payment.repository.PaymentRepository;
import com.back.b2st.domain.reservation.service.ReservationService;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentFailService {

	private final PaymentRepository paymentRepository;
	private final ReservationService reservationService;

	@Transactional
	public Payment fail(Long memberId, String orderId, String reason) {
		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.NOT_FOUND));

		validateOwner(payment, memberId);

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
		if (payment.getDomainType() == DomainType.RESERVATION) {
			reservationService.failReservation(payment.getDomainId());
		}
	}

	private void validateOwner(Payment payment, Long memberId) {
		if (!payment.getMemberId().equals(memberId)) {
			throw new BusinessException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
		}
	}
}

