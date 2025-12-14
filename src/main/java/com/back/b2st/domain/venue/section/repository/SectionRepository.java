package com.back.b2st.domain.venue.section.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.back.b2st.domain.venue.section.entity.Section;

public interface SectionRepository extends JpaRepository<Section, Long> {
}
