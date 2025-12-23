package com.back.b2st.domain.reservation.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationStatus;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, ReservationRepositoryCustom {

	/** === 로그인 유저 예매 전체 조회 === */
	List<Reservation> findAllByMemberId(Long memberId);

	/** 활성 PENDING 존재 여부(만료된 PENDING은 제외) */
	boolean existsByScheduleIdAndSeatIdAndStatusAndExpiresAtAfter(
		Long scheduleId,
		Long seatId,
		ReservationStatus status,
		LocalDateTime now
	);

	/** COMPLETED 존재 여부(완료는 언제나 중복 방지) */
	boolean existsByScheduleIdAndSeatIdAndStatus(
		Long scheduleId,
		Long seatId,
		ReservationStatus status
	);

	/** 상태 변경 경쟁(complete/fail/expire) 직렬화를 위한 락 조회 */
	//@Lock(LockModeType.PESSIMISTIC_WRITE)
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
