package com.back.b2st.domain.venue.venue.entity;

import com.back.b2st.global.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "venue")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
		name = "venue_id_gen",
		sequenceName = "venue_seq",
		allocationSize = 50
)
public class Venue extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "venue_id_gen")
	@Column(name = "venue_id")
	private Long venueId;

	@Column(nullable = false, length = 200)
	private String name;

	@Builder
	public Venue(String name) {
		this.name = name;
	}
}

