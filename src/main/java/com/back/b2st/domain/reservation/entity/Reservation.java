package com.back.b2st.domain.reservation.entity;

import com.back.b2st.global.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "reservation")
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

	@Column(name = "performance_id", nullable = false)
	private Long performanceId;    // 공연 FK

	@Column(name = "member_id", nullable = false)
	private Long memberId;    // 예매자 FK

	@Column(name = "seat_id", nullable = false)
	private Long seatId;    // 좌석 FK

	/** === 예매 상태 === */
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private ReservationStatus status;

	@Builder
	public Reservation(
		Long performanceId,
		Long memberId,
		Long seatId
	) {
		this.performanceId = performanceId;
		this.memberId = memberId;
		this.seatId = seatId;
	}

	/** === 상태 변경 === */
	public void markPaid() {
		this.status = ReservationStatus.PAID;
	}
}
