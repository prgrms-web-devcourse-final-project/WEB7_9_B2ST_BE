package com.back.b2st.domain.payment.service;

import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.payment.dto.request.PaymentCancelReq;
import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentStatus;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.payment.repository.PaymentRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentCancelService {

	private final PaymentRepository paymentRepository;
	private final Clock clock;

	@Transactional
	public Payment cancel(Long memberId, String orderId, PaymentCancelReq request) {
		// 1. 결제 조회
		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.NOT_FOUND));

		// 2. 권한 검증 (본인 결제만 취소 가능)
		payment.validateOwner(memberId);

		// 3. 멱등성 처리: 이미 취소된 경우
		if (payment.getStatus() == PaymentStatus.CANCELED) {
			return payment;
		}

		// 4. DONE 상태만 취소 가능
		if (payment.getStatus() != PaymentStatus.DONE) {
			throw new BusinessException(PaymentErrorCode.INVALID_STATUS,
				"완료된 결제만 취소할 수 있습니다.");
		}

		// 5. 정책: 티켓 거래 결제는 취소/환불 미지원
		if (payment.getDomainType() == DomainType.TRADE) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE,
				"티켓 거래 결제는 취소/환불을 지원하지 않습니다.");
		}

		// 6. 정책: 예매 결제는 취소/환불 미지원 (결제 완료 후 예매 취소 불가)
		if (payment.getDomainType() == DomainType.RESERVATION) {
			throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE,
				"예매 결제는 취소/환불을 지원하지 않습니다.");
		}

		// 7. 현재 지원하지 않는 도메인인 경우
		throw new BusinessException(PaymentErrorCode.DOMAIN_NOT_PAYABLE,
			"결제 취소를 지원하지 않는 도메인입니다.");
	}
}
