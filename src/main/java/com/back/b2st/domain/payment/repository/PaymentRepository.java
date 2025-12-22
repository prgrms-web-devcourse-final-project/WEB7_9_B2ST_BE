package com.back.b2st.domain.payment.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	/**
	 * 주문 ID로 결제 조회 (유니크)
	 */
	Optional<Payment> findByOrderId(String orderId);

	/**
	 * 결제 키로 결제 조회 (내부 추적용, 유니크)
	 */
	Optional<Payment> findByPaymentKey(String paymentKey);

	/**
	 * 회원의 모든 결제 내역 조회 (최신순)
	 */
	List<Payment> findByMemberIdOrderByCreatedAtDesc(Long memberId);

	/**
	 * 특정 도메인의 모든 결제 시도 조회 (결제 재시도 시 여러 건 존재 가능)
	 * 예: 예매(BOOKING) ID 123에 대한 모든 결제 시도 내역
	 */
	List<Payment> findByDomainTypeAndDomainId(DomainType domainType, Long domainId);

	/**
	 * 특정 도메인의 최신 결제 시도 조회 (결제 중복 방지 및 현재 상태 확인용)
	 * 예: 예매(BOOKING) ID 123의 가장 최근 결제 시도
	 */
	Optional<Payment> findTopByDomainTypeAndDomainIdOrderByCreatedAtDesc(DomainType domainType, Long domainId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Payment p "
		+ "SET p.status = com.back.b2st.domain.payment.entity.PaymentStatus.DONE, "
		+ "p.paidAt = :paidAt "
		+ "WHERE p.orderId = :orderId "
		+ "AND (p.status = com.back.b2st.domain.payment.entity.PaymentStatus.READY "
		+ "OR p.status = com.back.b2st.domain.payment.entity.PaymentStatus.WAITING_FOR_DEPOSIT)")
	int completeIfReady(
		@Param("orderId") String orderId,
		@Param("paidAt") LocalDateTime paidAt
	);

	/**
	 * 만료된 무통장 입금 대기 건 조회 (배치 작업용)
	 * @param status 조회할 상태 (일반적으로 WAITING_FOR_DEPOSIT)
	 * @param now 현재 시각
	 */
	@Query("SELECT p FROM Payment p "
		+ "WHERE p.status = :status "
		+ "AND p.expiresAt IS NOT NULL "
		+ "AND p.expiresAt < :now")
	List<Payment> findExpiredPayments(
		@Param("status") PaymentStatus status,
		@Param("now") LocalDateTime now
	);

	/**
	 * 특정 상태의 결제 건 조회
	 */
	List<Payment> findByStatus(PaymentStatus status);

	/**
	 * 회원의 특정 도메인 결제 내역 조회 (최신순)
	 * 예: 회원 ID 1의 모든 예매(BOOKING) 결제 내역
	 */
	List<Payment> findByMemberIdAndDomainTypeOrderByCreatedAtDesc(
		Long memberId,
		DomainType domainType
	);
}
