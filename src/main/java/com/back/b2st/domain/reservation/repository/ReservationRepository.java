package com.back.b2st.domain.reservation.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	List<Reservation> findAllByMemberIdAndStatus(Long memberId, ReservationStatus status);

	Optional<Reservation> findTopByMemberIdAndScheduleIdAndStatusOrderByIdDesc(
		Long memberId,
		Long scheduleId,
		ReservationStatus status
	);

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

	@Query("""
		select r.id
		  from Reservation r
		 where r.scheduleId in :scheduleIds
		""")
	List<Long> findIdsByScheduleIdIn(@Param("scheduleIds") List<Long> scheduleIds);

	void deleteAllByScheduleIdIn(List<Long> scheduleIds);

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

	@Query("""
			select r
			from Reservation r
			where r.status = :status
			  and (:scheduleId is null or r.scheduleId = :scheduleId)
			  and (:memberId is null or r.memberId = :memberId)
			order by r.id desc
		""")
	Page<Reservation> findByStatusWithOptionalFilters(
		@Param("status") ReservationStatus status,
		@Param("scheduleId") Long scheduleId,
		@Param("memberId") Long memberId,
		Pageable pageable
	);

}
