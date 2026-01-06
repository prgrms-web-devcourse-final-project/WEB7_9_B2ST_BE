package com.back.b2st.domain.seat.seat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.b2st.domain.seat.seat.dto.response.SeatInfoRes;
import com.back.b2st.domain.seat.seat.entity.Seat;

public interface SeatRepository extends JpaRepository<Seat, Long> {

	List<Seat> findByVenueId(Long venueId);

	@Query(value = """
			SELECT new com.back.b2st.domain.seat.seat.dto.response.SeatInfoRes(
				s.id, s.sectionName, s.rowLabel, s.seatNumber, g.grade, g.price
			)
			FROM Seat s
			JOIN SeatGrade g ON g.seatId = s.id
			WHERE s.sectionId = :sectionId
		""")
	List<SeatInfoRes> findSeatInfoResBySectionId(@Param("sectionId") Long sectionId);

	boolean existsByVenueIdAndSectionIdAndSectionNameAndRowLabelAndSeatNumber(
		Long venueId, Long sectionId, String sectionName, String rowLabel, Integer seatNumber);

	@Query("""
			SELECT new com.back.b2st.domain.seat.seat.dto.response.SeatInfoRes(
					s.id, s.sectionName, s.rowLabel, s.seatNumber, g.grade, g.price
			)
			FROM Seat s
			JOIN SeatGrade g ON g.seatId = s.id
			WHERE s.venueId = :venueId
		""")
	List<SeatInfoRes> findSeatInfoResByVenueId(@Param("venueId") Long venudId);

	@Query("""
			SELECT new com.back.b2st.domain.seat.seat.dto.response.SeatInfoRes(
					s.id, s.sectionName, s.rowLabel, s.seatNumber, g.grade, g.price
			)
			FROM Seat s
			JOIN SeatGrade g ON g.seatId = s.id
			WHERE s.id = :id
		""")
	Optional<SeatInfoRes> findSeatInfoResById(@Param("id") Long id);
}
