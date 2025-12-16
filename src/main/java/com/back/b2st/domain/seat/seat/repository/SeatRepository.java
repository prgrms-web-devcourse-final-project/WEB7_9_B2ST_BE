package com.back.b2st.domain.seat.seat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.seat.seat.entity.Seat;

public interface SeatRepository extends JpaRepository<Seat, Long> {
	List<Seat> findBySectionId(Long sectionId);

	boolean existsByVenueIdAndSectionIdAndSectionNameAndRowLabelAndSeatNumber(
		Long venueId, Long sectionId, String sectionName, String rowLabel, Integer seatNumber);

	List<Seat> findByVenueId(Long venudId);
}
