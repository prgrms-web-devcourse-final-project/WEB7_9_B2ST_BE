package com.back.b2st.domain.prereservation.booking.repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.back.b2st.domain.prereservation.booking.entity.PrereservationBooking;
import com.back.b2st.domain.prereservation.booking.entity.PrereservationBookingStatus;

import jakarta.persistence.LockModeType;

@Repository
public interface PrereservationBookingRepository extends JpaRepository<PrereservationBooking, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select b from PrereservationBooking b where b.id = :id")
	Optional<PrereservationBooking> findByIdWithLock(@Param("id") Long id);

	@Query("""
		select b
		  from PrereservationBooking b
		 where b.scheduleSeatId = :scheduleSeatId
		   and b.status in :activeStatuses
		""")
	Optional<PrereservationBooking> findActiveByScheduleSeatId(
		@Param("scheduleSeatId") Long scheduleSeatId,
		@Param("activeStatuses") List<PrereservationBookingStatus> activeStatuses
	);

	@Query("""
		select b
		  from PrereservationBooking b
		 where b.scheduleSeatId = :scheduleSeatId
		   and b.status in :activeStatuses
		   and b.expiresAt > :now
		""")
	Optional<PrereservationBooking> findActiveByScheduleSeatIdAndNotExpired(
		@Param("scheduleSeatId") Long scheduleSeatId,
		@Param("activeStatuses") List<PrereservationBookingStatus> activeStatuses,
		@Param("now") LocalDateTime now
	);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update PrereservationBooking b
		   set b.status = :expiredStatus
		 where b.status = :targetStatus
		   and b.expiresAt <= :now
		""")
	int expireCreatedBookingsBatch(
		@Param("targetStatus") PrereservationBookingStatus targetStatus,
		@Param("expiredStatus") PrereservationBookingStatus expiredStatus,
		@Param("now") LocalDateTime now
	);

	@Query("""
		select b.id
		  from PrereservationBooking b
		 where b.scheduleId in :scheduleIds
		""")
	List<Long> findIdsByScheduleIdIn(@Param("scheduleIds") List<Long> scheduleIds);

	void deleteAllByScheduleIdIn(List<Long> scheduleIds);
}
