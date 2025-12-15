package com.back.b2st.domain.performanceschedule.entity;

import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.global.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "performance_schedule")
@SequenceGenerator(
		name = "performance_schedule_id_gen",
		sequenceName = "performance_schedule_seq",
		allocationSize = 50
)
public class PerformanceSchedule extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "performance_schedule_id_gen")
	@Column(name = "performance_schedule_id")
	private Long performanceScheduleId;    // PK

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "performance_id", nullable = false)
	private Performance performance;        // 공연 FK

	@Column(name = "start_at", nullable = false)
	private LocalDateTime startAt;           // 공연 시작 일시

	@Column(name = "round_no", nullable = false)
	private Integer roundNo;                 // 회차 번호

	/** === 예매 방식 === */
	@Enumerated(EnumType.STRING)
	@Column(name = "booking_type", nullable = false)
	private BookingType bookingType;

	@Column(name = "booking_open_at", nullable = false)
	private LocalDateTime bookingOpenAt;     // 예매 오픈 시각

	@Column(name = "booking_close_at")
	private LocalDateTime bookingCloseAt;    // 예매 마감 시각

	@Builder
	public PerformanceSchedule(
			Performance performance,
			LocalDateTime startAt,
			Integer roundNo,
			BookingType bookingType,
			LocalDateTime bookingOpenAt,
			LocalDateTime bookingCloseAt
	) {
		this.performance = performance;
		this.startAt = startAt;
		this.roundNo = roundNo;
		this.bookingType = bookingType;
		this.bookingOpenAt = bookingOpenAt;
		this.bookingCloseAt = bookingCloseAt;
	}
}
