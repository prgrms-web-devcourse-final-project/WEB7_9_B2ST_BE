package com.back.b2st.domain.reservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.reservation.entity.ScheduleSeat;

public interface ScheduleSeatRepository extends JpaRepository<ScheduleSeat, Long> {
}
