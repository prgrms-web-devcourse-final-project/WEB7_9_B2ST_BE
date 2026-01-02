package com.back.b2st.domain.prereservation.booking.entity;

import java.time.LocalDateTime;

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
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "prereservation_booking",
	indexes = {
		@Index(name = "idx_prereservation_booking_member", columnList = "member_id"),
		@Index(name = "idx_prereservation_booking_schedule_seat", columnList = "schedule_seat_id")
	}
)
@SequenceGenerator(
	name = "prereservation_booking_id_gen",
	sequenceName = "prereservation_booking_seq",
	allocationSize = 50
)
public class PrereservationBooking extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "prereservation_booking_id_gen")
	@Column(name = "prereservation_booking_id")
	private Long id;

	@Column(name = "schedule_id", nullable = false)
	private Long scheduleId;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "schedule_seat_id", nullable = false)
	private Long scheduleSeatId;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private PrereservationBookingStatus status;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Column(name = "canceled_at")
	private LocalDateTime canceledAt;

	@Builder
	public PrereservationBooking(Long scheduleId, Long memberId, Long scheduleSeatId, LocalDateTime expiresAt) {
		this.scheduleId = scheduleId;
		this.memberId = memberId;
		this.scheduleSeatId = scheduleSeatId;
		this.expiresAt = expiresAt;
		this.status = PrereservationBookingStatus.CREATED;
	}

	public void complete(LocalDateTime completedAt) {
		this.status = PrereservationBookingStatus.COMPLETED;
		this.completedAt = completedAt;
	}

	public void cancel(LocalDateTime canceledAt) {
		this.status = PrereservationBookingStatus.CANCELED;
		this.canceledAt = canceledAt;
	}

	public void fail() {
		this.status = PrereservationBookingStatus.FAILED;
	}
}

