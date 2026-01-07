package com.back.b2st.domain.reservation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.back.b2st.domain.reservation.entity.ReservationSeat;

@Repository
public interface ReservationSeatRepository
	extends JpaRepository<ReservationSeat, Long>, ReservationSeatRepositoryCustom {

	List<ReservationSeat> findByReservationId(Long reservationId);

	boolean existsByReservationId(Long reservationId);

	int countByReservationId(Long reservationId);
}