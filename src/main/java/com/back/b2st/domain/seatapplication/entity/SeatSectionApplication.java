package com.back.b2st.domain.seatapplication.entity;

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
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "seat_section_applications",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_seat_section_applications_schedule_member_section",
			columnNames = {"performance_schedule_id", "member_id", "section_id"}
		)
	}
)
@SequenceGenerator(
	name = "seat_section_applications_id_gen",
	sequenceName = "seat_section_applications_seq",
	allocationSize = 50
)
public class SeatSectionApplication extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seat_section_applications_id_gen")
	@Column(name = "seat_section_application_id")
	private Long id;

	@Column(name = "performance_schedule_id", nullable = false)
	private Long performanceScheduleId;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "section_id", nullable = false)
	private Long sectionId;

	@Builder
	public SeatSectionApplication(Long performanceScheduleId, Long memberId, Long sectionId) {
		this.performanceScheduleId = performanceScheduleId;
		this.memberId = memberId;
		this.sectionId = sectionId;
	}
}
