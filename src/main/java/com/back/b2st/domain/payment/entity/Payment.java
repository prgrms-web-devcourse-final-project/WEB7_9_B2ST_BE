package com.back.b2st.domain.payment.entity;

import java.time.Clock;
import java.time.LocalDateTime;

import com.back.b2st.domain.payment.error.PaymentErrorCode;
import com.back.b2st.global.error.exception.BusinessException;
import com.back.b2st.global.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment",
	indexes = {
		@Index(name = "uk_payment_order_id", columnList = "order_id", unique = true),
		@Index(name = "uk_payment_payment_key", columnList = "payment_key", unique = true),
		@Index(name = "idx_payment_member_created", columnList = "member_id, created_at"),
		@Index(name = "idx_payment_domain", columnList = "domain_type, domain_id"),
		@Index(name = "idx_payment_expires", columnList = "status, expires_at"),
		@Index(name = "idx_payment_method_status", columnList = "method, status, created_at")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
	name = "payment_id_gen",
	sequenceName = "payment_seq",
	allocationSize = 50
)
public class Payment extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_id_gen")
	@Column(name = "payment_id")
	private Long id;

	@Column(name = "order_id", nullable = false, length = 100)
	private String orderId;

	/**
	 * 결제 키 (내부 추적용)
	 * MVP: 외부 PG 연동 없음, null 가능
	 */
	@Column(name = "payment_key", length = 200)
	private String paymentKey;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Enumerated(EnumType.STRING)
	@Column(name = "domain_type", nullable = false, length = 20)
	private DomainType domainType;

	@Column(name = "domain_id", nullable = false)
	private Long domainId;

	@Column(name = "amount", nullable = false)
	private Long amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "method", nullable = false, length = 20)
	private PaymentMethod method;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private PaymentStatus status;

	/**
	 * 무통장 입금 만료 시각
	 * MVP: 사용 안 함 (입금 만료 처리 없음)
	 */
	@Column(name = "expires_at")
	private LocalDateTime expiresAt;

	@Column(name = "paid_at")
	private LocalDateTime paidAt;

	@Column(name = "canceled_at")
	private LocalDateTime canceledAt;

	@Column(name = "failure_reason", columnDefinition = "TEXT")
	private String failureReason;

	@Builder
	public Payment(String orderId, Long memberId, DomainType domainType, Long domainId,
		Long amount, PaymentMethod method, LocalDateTime expiresAt) {
		this.orderId = orderId;
		this.memberId = memberId;
		this.domainType = domainType;
		this.domainId = domainId;
		this.amount = amount;
		this.method = method;
		this.expiresAt = expiresAt;

		// 결제 수단에 따라 초기 상태 결정
		this.status = method.isRequiresDeposit()
			? PaymentStatus.WAITING_FOR_DEPOSIT
			: PaymentStatus.READY;
	}

	// === 상태 전이 메서드 ===

	/**
	 * 결제 승인 완료 처리
	 * READY → DONE 또는 WAITING_FOR_DEPOSIT → DONE
	 */
	public void complete(LocalDateTime paidAt) {
		complete(null, paidAt);
	}

	public void complete(String paymentKey, LocalDateTime paidAt) {
		validateTransition(PaymentStatus.DONE);

		this.paymentKey = paymentKey;
		this.status = PaymentStatus.DONE;
		this.paidAt = paidAt;
	}

	/**
	 * 결제 실패 처리
	 * READY → FAILED
	 */
	public void fail(String reason) {
		validateTransition(PaymentStatus.FAILED);

		this.status = PaymentStatus.FAILED;
		this.failureReason = reason;
	}

	/**
	 * 결제 취소 처리 (환불)
	 * DONE → CANCELED 또는 READY → CANCELED
	 */
	public void cancel(String reason, LocalDateTime canceledAt) {
		validateTransition(PaymentStatus.CANCELED);

		this.status = PaymentStatus.CANCELED;
		this.canceledAt = canceledAt;
		this.failureReason = reason;
	}

	/**
	 * 무통장 입금 만료 처리
	 * WAITING_FOR_DEPOSIT → EXPIRED
	 */
	public void expire() {
		validateTransition(PaymentStatus.EXPIRED);

		this.status = PaymentStatus.EXPIRED;
		this.failureReason = "입금 기한 초과";
	}

	/**
	 * 상태 전이 가능 여부 검증 (상태 전이표 기반)
	 */
	private void validateTransition(PaymentStatus targetStatus) {
		if (status.isFinal()) {
			throw new BusinessException(PaymentErrorCode.INVALID_STATUS,
				String.format("현재 상태(%s)에서는 더 이상 상태 변경이 불가능합니다.", status));
		}

		if (!status.canTransitionTo(targetStatus)) {
			throw new BusinessException(PaymentErrorCode.INVALID_STATUS,
				String.format("현재 상태(%s)에서 %s 상태로 전이할 수 없습니다.", status, targetStatus));
		}
	}

	/**
	 * 금액 검증 (변조 방지)
	 */
	public void validateAmount(Long requestAmount) {
		if (!this.amount.equals(requestAmount)) {
			throw new BusinessException(PaymentErrorCode.AMOUNT_MISMATCH,
				String.format("금액이 일치하지 않습니다. 예상: %d, 요청: %d", this.amount, requestAmount));
		}
	}

	/**
	 * 소유자 검증 (권한 확인)
	 */
	public void validateOwner(Long requestMemberId) {
		if (!this.memberId.equals(requestMemberId)) {
			throw new BusinessException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
		}
	}

	/**
	 * 무통장 입금 만료 여부 확인
	 */
	public boolean isExpired(LocalDateTime now) {
		return expiresAt != null && now.isAfter(expiresAt);
	}

	public boolean isExpired(Clock clock) {
		return isExpired(LocalDateTime.now(clock));
	}
}
