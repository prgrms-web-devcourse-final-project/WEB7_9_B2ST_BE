package com.back.b2st.domain.scheduleseat.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;

import jakarta.persistence.LockModeType;

public interface ScheduleSeatRepository extends JpaRepository<ScheduleSeat, Long>, ScheduleSeatRepositoryCustom {

	/** scheduleId + seatId 로 특정 좌석 조회 */
	Optional<ScheduleSeat> findByScheduleIdAndSeatId(Long scheduleId, Long seatId);

	/** 만료된 HOLD 좌석 목록 조회(키 추출) */
	@Query("""
		select s.scheduleId, s.seatId
		  from ScheduleSeat s
		 where s.status = :hold
		   and s.holdExpiredAt is not null
		   and s.holdExpiredAt <= :now
		""")
	List<Object[]> findExpiredHoldKeys(@Param("hold") SeatStatus hold, @Param("now") LocalDateTime now);

	/** HOLD 만료 좌석 -> AVAILABLE로 일괄 처리 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update ScheduleSeat s
		   set s.status = :available,
		       s.holdExpiredAt = null
		 where s.status = :hold
		   and s.holdExpiredAt is not null
		   and s.holdExpiredAt <= :now
		""")
	int releaseExpiredHolds(
		@Param("hold") SeatStatus hold,
		@Param("available") SeatStatus available,
		@Param("now") LocalDateTime now);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM ScheduleSeat s WHERE s.scheduleId = :scheduleId AND s.seatId = :seatId")
	Optional<ScheduleSeat> findByScheduleIdAndSeatIdWithLock(
		@Param("scheduleId") Long scheduleId,
		@Param("seatId") Long seatId
	);
}
