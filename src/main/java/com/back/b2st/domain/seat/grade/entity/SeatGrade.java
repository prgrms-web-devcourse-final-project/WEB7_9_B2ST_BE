package com.back.b2st.domain.seat.grade.entity;

import com.back.b2st.global.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(
	name = "seat_grades",
	indexes = {
		@Index(name = "idx_performance", columnList = "performance_id"),
		@Index(
			name = "idx_performance_seat_grade",
			columnList = "performance_id, seat_id, grade"
		)
	}
)
@SequenceGenerator(
	name = "seat_grades_id_gen",
	sequenceName = "SEAT_GRADE_SEQ",
	allocationSize = 50
)
public class SeatGrade extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seat_id_gen")
	@Column(name = "seat_grade_id")
	private Long id;

	@Column(name = "performance_id", nullable = false)
	private Long performanceId;

	@Column(name = "seat_id", nullable = false)
	private Long seatId;

	@Enumerated(EnumType.STRING)
	@Column(name = "grade", nullable = false, length = 20)
	private SeatGradeType grade;

	@Column(name = "price", nullable = false)
	private Integer price;

	@Builder
	public SeatGrade(Long performanceId, Long seatId, SeatGradeType grade, Integer price) {
		this.performanceId = performanceId;
		this.seatId = seatId;
		this.grade = grade;
		this.price = price;
	}
}
