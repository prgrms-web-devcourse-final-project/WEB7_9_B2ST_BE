package com.back.b2st.domain.ticket.entity;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.back.b2st.global.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(
	name = "tickets",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_tickets_reservation_member_seat",
		columnNames = {"reservation_id", "member_id", "seat_id"}
	),
	indexes = {
		@Index(name = "idx_tickets_reservation_member", columnList = "reservation_id, member_id"),
		@Index(name = "idx_tickets_member", columnList = "member_id")
	})
@EntityListeners(AuditingEntityListener.class)
@SequenceGenerator(
	name = "ticket_id_gen",
	sequenceName = "TICKET_SEQ",
	allocationSize = 50
)
public class Ticket extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ticket_id_gen")
	@Column(name = "ticket_id")
	private Long id;

	@Column(name = "reservation_id", nullable = false)
	private Long reservationId;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private TicketStatus status;

	@Column(name = "seat_id", nullable = false)
	private Long seatId;

	@Column(name = "qr_code", unique = true)
	private String qrCode;

	@Builder
	public Ticket(
		Long reservationId,
		Long memberId,
		Long seatId,
		String qrCode
	) {
		this.reservationId = reservationId;
		this.memberId = memberId;
		this.seatId = seatId;
		this.qrCode = qrCode;
		this.status = TicketStatus.ISSUED;
	}
}
