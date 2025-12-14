package com.back.b2st.domain.venue.section.entity;

import com.back.b2st.global.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(
	name = "sections",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_section_venue_name",
			columnNames = {"venue_id", "section_name"}
		)
	}
)
@SequenceGenerator(
	name = "section_id_gen",
	sequenceName = "section_seq",
	allocationSize = 50
)
public class Section extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "section_id_gen")
	@Column(name = "section_id")
	private Long id;

	@Column(name = "venue_id", nullable = false)
	private Long venueId;

	@Column(name = "section_name", nullable = false, length = 20)
	private String sectionName;

	@Builder
	public Section(Long venueId, String sectionName) {
		this.venueId = venueId;
		this.sectionName = sectionName;
	}
}
