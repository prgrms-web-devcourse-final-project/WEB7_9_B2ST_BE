package com.back.b2st.domain.reservation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.back.b2st.domain.reservation.entity.Reservation;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, ReservationRepositoryCustom {

	/** === 로그인 유저 예매 전체 조회 === */
	List<Reservation> findAllByMemberId(Long memberId);

	/** === 좌석 중복 예매 방지 === */
	boolean existsByScheduleIdAndSeatId(Long scheduleId, Long seatId);
}
