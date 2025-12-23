package com.back.b2st.domain.reservation.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationStatus;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, ReservationRepositoryCustom {

	/** === 로그인 유저 예매 전체 조회 === */
	List<Reservation> findAllByMemberId(Long memberId);

	/** === 좌석 중복 예매 방지 === */
	boolean existsByScheduleIdAndSeatIdAndStatusIn(Long scheduleId, Long seatId, List<ReservationStatus> statuses);

	/** === 만료 대상 조회 메서드 추가 + 활성 중복 체크 유지 === */
	List<Reservation> findAllByStatusAndExpiresAtLessThanEqual(ReservationStatus status, LocalDateTime now);

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
}
