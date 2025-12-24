package com.back.b2st.domain.payment.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.payment.dto.response.PaymentConfirmRes;
import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentViewService {

	private final PaymentRepository paymentRepository;

	@Transactional(readOnly = true)
	public PaymentConfirmRes getByReservationId(Long reservationId, Long memberId) {
		return paymentRepository.findByDomainTypeAndDomainIdAndMemberId(
				DomainType.RESERVATION,
				reservationId,
				memberId
			)
			.map(PaymentConfirmRes::from)
			.orElse(null);
	}
}
