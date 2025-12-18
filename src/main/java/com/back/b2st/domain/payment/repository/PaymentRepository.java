package com.back.b2st.domain.payment.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByOrderId(String orderId);

	Optional<Payment> findByPaymentKey(String paymentKey);

	List<Payment> findByMemberIdOrderByCreatedAtDesc(Long memberId);

	Optional<Payment> findByDomainTypeAndDomainId(DomainType domainType, Long domainId);

	@Query("SELECT p FROM Payment p " +
		"WHERE p.status = :status " +
		"AND p.expiresAt IS NOT NULL " +
		"AND p.expiresAt < :now")
	List<Payment> findExpiredPayments(
		@Param("status") PaymentStatus status,
		@Param("now") LocalDateTime now
	);

	List<Payment> findByStatus(PaymentStatus status);

	List<Payment> findByMemberIdAndDomainTypeOrderByCreatedAtDesc(
		Long memberId,
		DomainType domainType
	);
}
