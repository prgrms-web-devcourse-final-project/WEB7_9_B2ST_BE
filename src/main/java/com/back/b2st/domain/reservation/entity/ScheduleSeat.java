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
@Table(
	name = "schedule_seat",
	indexes = {
		@Index(name = "idx_schedule_seat_schedule", columnList = "schedule_id"),
		@Index(name = "idx_schedule_seat_status", columnList = "status")
	}
)
@SequenceGenerator(
	name = "schedule_seat_id_gen",
	sequenceName = "schedule_seat_seq",
	allocationSize = 50
)
public class ScheduleSeat extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "schedule_seat_id_gen")
	@Column(name = "schedule_seat_id")
	private Long id;

	@Column(name = "schedule_id", nullable = false)
	private Long scheduleId; // 회차 FK

	@Column(name = "seat_id", nullable = false)
	private Long seatId; // 좌석 FK

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private SeatStatus status;

	@Builder
	public ScheduleSeat(Long scheduleId, Long seatId) {
		this.scheduleId = scheduleId;
		this.seatId = seatId;
		this.status = SeatStatus.AVAILABLE;
	}

	/* === 상태 전환 메서드 === */

	/** HOLD 상태로 변경 */
	public void hold(LocalDateTime expireAt) {
		this.status = SeatStatus.HOLD;
	}

	/** SOLD(예매 확정) 상태로 변경 */
	public void markSold() {
		this.status = SeatStatus.SOLD;
	}

	/** AVAILABLE 상태로 복구 (취소) */
	public void release() {
		this.status = SeatStatus.AVAILABLE;
	}

	/** HOLD 상태인지 확인 */
	public boolean isHold() {
		return this.status == SeatStatus.HOLD;
	}
}