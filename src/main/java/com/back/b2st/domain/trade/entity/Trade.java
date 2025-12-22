package com.back.b2st.domain.trade.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trade",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_trade_ticket_active",
		columnNames = {"ticket_id", "status"}
	),
	indexes = {
		@Index(name = "idx_trade_member_status", columnList = "member_id, status"),
		@Index(name = "idx_trade_performance", columnList = "performance_id, status"),
		@Index(name = "idx_trade_type_status_created", columnList = "type, status, created_at"),
		@Index(name = "idx_trade_status_created", columnList = "status, created_at"),
		@Index(name = "idx_trade_type_created", columnList = "type, created_at")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
	name = "trade_id_gen",
	sequenceName = "trade_seq",
	allocationSize = 50
)
public class Trade extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trade_id_gen")
	@Column(name = "trade_id")
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "performance_id", nullable = false)
	private Long performanceId;

	@Column(name = "schedule_id", nullable = false)
	private Long scheduleId;

	@Column(name = "ticket_id", nullable = false)
	private Long ticketId;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private TradeType type;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private TradeStatus status;

	@Column(name = "price")
	private Integer price;

	@Column(name = "total_count", nullable = false)
	private Integer totalCount;

	@Column(name = "section", nullable = false)
	private String section;  // 구역 (예: A, B, VIP)

	@Column(name = "row_name", nullable = false)
	private String row;  // 열 (예: 5열, 10열)

	@Column(name = "seat_number", nullable = false)
	private String seatNumber;  // 좌석 번호 (예: 12석, 25석)

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Builder
	public Trade(Long memberId, Long performanceId, Long scheduleId, Long ticketId,
		TradeType type, Integer price, Integer totalCount,
		String section, String row, String seatNumber) {
		this.memberId = memberId;
		this.performanceId = performanceId;
		this.scheduleId = scheduleId;
		this.ticketId = ticketId;
		this.type = type;
		this.status = TradeStatus.ACTIVE;
		this.price = price;
		this.totalCount = totalCount;
		this.section = section;
		this.row = row;
		this.seatNumber = seatNumber;
	}

	public void updatePrice(Integer newPrice) {
		this.price = newPrice;
	}

	public void complete() {
		this.status = TradeStatus.COMPLETED;
	}

	public void cancel() {
		this.status = TradeStatus.CANCELLED;
		this.deletedAt = LocalDateTime.now();
	}
}
