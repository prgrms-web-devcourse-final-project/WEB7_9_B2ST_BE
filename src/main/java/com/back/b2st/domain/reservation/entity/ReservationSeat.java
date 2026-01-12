package com.back.b2st.domain.reservation.entity;

import com.back.b2st.global.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "reservation_seat")
@SequenceGenerator(
	name = "reservation_seat_id_gen",
	sequenceName = "reservation_seat_seq",
	allocationSize = 50
)
public class ReservationSeat extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reservation_seat_id_gen")
	private Long id;

	@Column(nullable = false)
	private Long reservationId;

	@Column(nullable = false)
	private Long scheduleSeatId;

	@Builder
	public ReservationSeat(Long reservationId, Long scheduleSeatId) {
		this.reservationId = reservationId;
		this.scheduleSeatId = scheduleSeatId;
	}
}
