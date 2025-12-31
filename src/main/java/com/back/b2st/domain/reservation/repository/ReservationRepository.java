package com.back.b2st.domain.reservation.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationStatus;

import jakarta.persistence.LockModeType;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, ReservationRepositoryCustom {

	/** 락 조회 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT r FROM Reservation r WHERE r.id = :reservationId")
	Optional<Reservation> findByIdWithLock(@Param("reservationId") Long reservationId);

	/** 만료 대상 예약 ID만 가져오기 */
	@Query("""
		select r.id
		  from Reservation r
		 where r.status = :pending
		   and r.expiresAt is not null
		   and r.expiresAt <= :now
		""")
	List<Long> findExpiredPendingIds(@Param("pending") ReservationStatus pending, @Param("now") LocalDateTime now);

	/** PENDING -> EXPIRED 일괄 처리 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update Reservation r
		   set r.status = :expired
		 where r.id in :ids
		   and r.status = :pending
		""")
	int bulkExpirePendingByIds(
		@Param("ids") List<Long> ids,
		@Param("pending") ReservationStatus pending,
		@Param("expired") ReservationStatus expired
	);
}
