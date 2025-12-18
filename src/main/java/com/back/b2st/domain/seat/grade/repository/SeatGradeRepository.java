package com.back.b2st.domain.seat.grade.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.seat.grade.entity.SeatGrade;

public interface SeatGradeRepository extends JpaRepository<SeatGrade, Long> {
	Optional<SeatGrade> findTopByPerformanceIdAndSeatIdOrderByIdDesc(Long performanceId, Long seatId);
}
