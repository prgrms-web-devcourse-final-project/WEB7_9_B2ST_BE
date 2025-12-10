package com.back.b2st.domain.trade.entity;

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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trade_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
	name = "trade_request_id_gen",
	sequenceName = "trade_request_seq",
	allocationSize = 50
)
public class TradeRequest extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trade_request_id_gen")
	@Column(name = "trade_request_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "trade_id", nullable = false)
	private Trade trade;

	@Column(name = "requester_id", nullable = false)
	private Long requesterId;

	@Column(name = "requester_ticket_id", nullable = false)
	private Long requesterTicketId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private TradeRequestStatus status;

	@Builder
	public TradeRequest(Trade trade, Long requesterId, Long requesterTicketId) {
		this.trade = trade;
		this.requesterId = requesterId;
		this.requesterTicketId = requesterTicketId;
		this.status = TradeRequestStatus.PENDING;
	}

	public void accept() {
		this.status = TradeRequestStatus.ACCEPTED;
	}

	public void reject() {
		this.status = TradeRequestStatus.REJECTED;
	}
}
