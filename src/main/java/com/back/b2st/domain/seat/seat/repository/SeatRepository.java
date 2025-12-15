package com.back.b2st.domain.seat.seat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.seat.seat.entity.Seat;

public interface SeatRepository extends JpaRepository<Seat, Long> {
	List<Seat> findBySectionId(Long sectionId);
}
