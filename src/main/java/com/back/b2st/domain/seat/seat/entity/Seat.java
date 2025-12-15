package com.back.b2st.domain.seat.seat.entity;

import com.back.b2st.global.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
	name = "seats",
	indexes = {
		@Index(name = "idx_seats_venue", columnList = "venue_id"),
		@Index(
			name = "idx_seats_venue_section_row_number",
			columnList = "venue_id, section_id, section_name, row_label, seat_number"
		),
		@Index(
			name = "idx_seats_venue_section",
			columnList = "venue_id, section_id, section_name"
		),
		@Index(
			name = "idx_seats_venue_section_row_number",
			columnList = "venue_id, section_id, section_name, row_label, seat_number"
		),
		@Index(
			name = "idx_seats_section",
			columnList = "section_id, section_name"
		)
	},
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_seats_venue_section_row_number",
			columnNames = {"venue_id", "section_id", "section_name", "row_label", "seat_number"}
		)
	}
)
@SequenceGenerator(
	name = "seat_id_gen",
	sequenceName = "SEAT_SEQ",
	allocationSize = 50
)
public class Seat extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seat_id_gen")
	@Column(name = "seat_id")
	private Long id;

	@Column(name = "venue_id", nullable = false)
	private Long venueId;    // 공연장 ID

	@Column(name = "section_id", nullable = false)
	private Long sectionId;

	@Column(name = "section_name", nullable = false, length = 20)
	private String sectionName;    // A구역 VIP

	@Column(name = "row_label", nullable = false, length = 5)
	private String rowLabel;    // 1열, A

	@Column(name = "seat_number", nullable = false)
	private Integer seatNumber;    // 7번

	@Builder
	public Seat(
		Long venueId,
		Long sectionId,
		String sectionName,
		String rowLabel,
		Integer seatNumber
	) {
		this.venueId = venueId;
		this.sectionId = sectionId;
		this.sectionName = sectionName;
		this.rowLabel = rowLabel;
		this.seatNumber = seatNumber;
	}
}
