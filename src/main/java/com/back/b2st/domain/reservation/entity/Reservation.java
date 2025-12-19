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

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private ReservationStatus status;

	@Builder
	public Reservation(
		Long scheduleId,
		Long memberId,
		Long seatId
	) {
		this.scheduleId = scheduleId;
		this.memberId = memberId;
		this.seatId = seatId;
		this.status = ReservationStatus.CREATED;
	}

	/** === 상태 가능 여부 판단 === */

	// 결제 대기 가능 여부
	public boolean canPending() {
		return this.status == ReservationStatus.CREATED;
	}

	//  결제 가능 여부
	public boolean canPay() {
		return this.status == ReservationStatus.CREATED
			|| this.status == ReservationStatus.PENDING;
	}

	// 확정 가능 여부
	public boolean canComplete() {
		return this.status == ReservationStatus.PAID;
	}

	// 취소 가능 여부
	public boolean canCancel() {
		return this.status == ReservationStatus.CREATED
			|| this.status == ReservationStatus.PENDING;
	}

	// 만료 가능 여부
	public boolean canExpire() {
		return this.status == ReservationStatus.CREATED
			|| this.status == ReservationStatus.PENDING;
	}

	/** === 상태 변경 === */
	public void pending() {
		this.status = ReservationStatus.PENDING;
	}

	public void paid() {
		this.status = ReservationStatus.PAID;
	}

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

	/** === 임시 호환 코드 === */
	@Deprecated
	public static class ReservationBuilder {

		public ReservationBuilder performanceId(Long performanceId) {
			this.scheduleId = performanceId;
			return this;
		}
	}

	@Deprecated
	public Long getPerformanceId() {
		return scheduleId;
	}
}
