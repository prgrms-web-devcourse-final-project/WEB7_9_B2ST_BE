package com.back.b2st.domain.venue.venue.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.venue.venue.entity.Venue;

public interface VenueRepository extends JpaRepository<Venue, Long> {
}
