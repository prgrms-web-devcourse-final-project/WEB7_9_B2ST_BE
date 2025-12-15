package com.back.b2st.domain.venue.seat.seat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.venue.seat.seat.entity.Seat;

public interface SeatRepository extends JpaRepository<Seat, Long> {
}
