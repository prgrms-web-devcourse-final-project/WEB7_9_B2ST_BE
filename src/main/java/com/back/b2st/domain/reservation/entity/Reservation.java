package com.back.b2st.domain.reservation.entity;

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
@Table(name = "reservation",
	indexes = {
		@Index(name = "idx_reservation_member", columnList = "member_id")
	}
)
@SequenceGenerator(
	name = "reservation_id_gen",
	sequenceName = "reservation_seq",
	allocationSize = 50
)
public class Reservation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reservation_id_gen")
	@Column(name = "reservation_id")
	private Long id;    // PK

	@Column(name = "schedule_id", nullable = false)
	private Long scheduleId;    // 회차 FK

	@Column(name = "member_id", nullable = false)
	private Long memberId;    // 예매자 FK

	@Column(name = "seat_id", nullable = false)
	private Long seatId;    // 좌석 FK

	@Column(name = "canceled_at")
	private LocalDateTime canceledAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private ReservationStatus status;

	@Builder
	public Reservation(
		Long scheduleId,
		Long memberId,
		Long seatId,
		LocalDateTime expiresAt
	) {
		this.scheduleId = scheduleId;
		this.memberId = memberId;
		this.seatId = seatId;
		this.expiresAt = expiresAt;
		this.status = ReservationStatus.PENDING;
	}

	/** === 상태 변경 === */
	public void cancel(LocalDateTime canceledAt) {
		this.status = ReservationStatus.CANCELED;
		this.canceledAt = canceledAt;
	}

	public void complete(LocalDateTime completedAt) {
		this.status = ReservationStatus.COMPLETED;
		this.completedAt = completedAt;
	}

	public void expire() {
		this.status = ReservationStatus.EXPIRED;
	}

	public void fail() {
		this.status = ReservationStatus.FAILED;
	}

	/** === 임시 호환 코드 === */
	@Deprecated
	public static class ReservationBuilder {

		public ReservationBuilder performanceId(Long performanceId) {
			this.scheduleId = performanceId;
			return this;
		}
	}
}
