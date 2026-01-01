package com.back.b2st.domain.prereservation.policy.entity;

import java.time.LocalDateTime;

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
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "prereservation_time_tables",
	indexes = {
		@Index(name = "idx_prereservation_time_tables_schedule", columnList = "performance_schedule_id"),
		@Index(name = "idx_prereservation_time_tables_schedule_section", columnList = "performance_schedule_id, section_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_prereservation_time_tables_schedule_section",
			columnNames = {"performance_schedule_id", "section_id"}
		)
	}
)
@SequenceGenerator(
	name = "prereservation_time_table_id_gen",
	sequenceName = "prereservation_time_table_seq",
	allocationSize = 50
)
public class PrereservationTimeTable extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "prereservation_time_table_id_gen")
	@Column(name = "prereservation_time_table_id")
	private Long id;

	@Column(name = "performance_schedule_id", nullable = false)
	private Long performanceScheduleId;

	@Column(name = "section_id", nullable = false)
	private Long sectionId;

	@Column(name = "booking_start_at", nullable = false)
	private LocalDateTime bookingStartAt;

	@Column(name = "booking_end_at", nullable = false)
	private LocalDateTime bookingEndAt;

	@Builder
	public PrereservationTimeTable(
		Long performanceScheduleId,
		Long sectionId,
		LocalDateTime bookingStartAt,
		LocalDateTime bookingEndAt
	) {
		this.performanceScheduleId = performanceScheduleId;
		this.sectionId = sectionId;
		this.bookingStartAt = bookingStartAt;
		this.bookingEndAt = bookingEndAt;
	}
}

