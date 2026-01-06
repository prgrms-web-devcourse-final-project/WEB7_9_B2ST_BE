package com.back.b2st.domain.prereservation.booking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

	void deleteAllByScheduleIdIn(List<Long> scheduleIds);
}
