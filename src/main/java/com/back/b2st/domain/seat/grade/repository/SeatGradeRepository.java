package com.back.b2st.domain.seat.grade.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.b2st.domain.seat.grade.dto.SeatCountByGrade;
import com.back.b2st.domain.seat.grade.entity.SeatGrade;
import com.back.b2st.domain.seat.grade.entity.SeatGradeType;

public interface SeatGradeRepository extends JpaRepository<SeatGrade, Long> {
	Optional<SeatGrade> findTopByPerformanceIdAndSeatIdOrderByIdDesc(Long performanceId, Long seatId);
	Optional<SeatGrade> findTopByPerformanceIdAndGradeOrderByIdDesc(Long performanceId, SeatGradeType grade);

	void deleteAllByPerformanceId(Long performanceId);

	/**
	 * 특정 공연의 등급별 좌석 수
	 */
	@Query("""
		select new com.back.b2st.domain.seat.grade.dto.SeatCountByGrade(
				sg.grade, count(sg.id)
		)
		from SeatGrade sg
		where sg.performanceId = :performanceId
		group by sg.grade
		""")
	List<SeatCountByGrade> countSeatGradesByGrade(@Param("performanceId") Long performanceId);
}
