package com.back.b2st.domain.payment.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.payment.dto.request.PaymentPrepareReq;
import com.back.b2st.domain.payment.dto.response.PaymentPrepareRes;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentMethod;
import com.back.b2st.domain.payment.entity.PaymentStatus;
import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.domain.payment.repository.PaymentRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentPrepareService {

	private static final Duration VIRTUAL_ACCOUNT_EXPIRES_IN = Duration.ofDays(1);

	private final PaymentRepository paymentRepository;
	private final List<PaymentDomainHandler> domainHandlers;

	@Transactional
	public PaymentPrepareRes prepare(Long memberId, PaymentPrepareReq request) {
		PaymentDomainHandler handler = domainHandlers.stream()
			.filter(h -> h.supports(request.domainType()))
			.findFirst()
			.orElseThrow(() -> new BusinessException(PaymentErrorCode.DOMAIN_NOT_FOUND));

		PaymentTarget target = handler.loadAndValidate(request.domainId(), memberId);
		validateNoDuplicatePayment(target);

		String orderId = UUID.randomUUID().toString();
		LocalDateTime expiresAt = buildExpiresAt(request.paymentMethod());

		Payment payment = Payment.builder()
			.orderId(orderId)
			.memberId(memberId)
			.domainType(target.domainType())
			.domainId(target.domainId())
			.amount(target.expectedAmount())
			.method(request.paymentMethod())
			.expiresAt(expiresAt)
			.build();

		Payment saved = paymentRepository.save(payment);

		return new PaymentPrepareRes(
			saved.getId(),
			saved.getOrderId(),
			saved.getAmount(),
			saved.getStatus(),
			saved.getExpiresAt()
		);
	}

	private void validateNoDuplicatePayment(PaymentTarget target) {
		paymentRepository.findTopByDomainTypeAndDomainIdOrderByCreatedAtDesc(target.domainType(), target.domainId())
			.ifPresent(existing -> {
				PaymentStatus status = existing.getStatus();
				if (status == PaymentStatus.DONE
					|| status == PaymentStatus.READY
					|| status == PaymentStatus.WAITING_FOR_DEPOSIT) {
					throw new BusinessException(PaymentErrorCode.DUPLICATE_PAYMENT);
				}
			});
	}

	private LocalDateTime buildExpiresAt(PaymentMethod method) {
		if (!method.isRequiresDeposit()) {
			return null;
		}
		return LocalDateTime.now().plus(VIRTUAL_ACCOUNT_EXPIRES_IN);
	}
}

