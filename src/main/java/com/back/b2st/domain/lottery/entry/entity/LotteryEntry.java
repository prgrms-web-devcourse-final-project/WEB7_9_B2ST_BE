package com.back.b2st.domain.lottery.entry.entity;

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
	name = "lottery_entries",
	indexes = {
		@Index(name = "idx_lottery_entries_member", columnList = "member_id"),
		@Index(name = "idx_lottery_entries_performance", columnList = "performance_id"),
		@Index(name = "idx_lottery_entries_schedule", columnList = "schedule_id"),
		@Index(name = "idx_lottery_entries_member_performance_schedule", columnList = "member_id, performance_id, schedule_id"),
		@Index(name = "idx_lottery_entries_status", columnList = "status")
	}
)
@SequenceGenerator(
	name = "lottery_entry_id_gen",
	sequenceName = "LOTTERY_ENTRY_SEQ",
	allocationSize = 50
)
public class LotteryEntry extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lottery_entry_id_gen")
	@Column(name = "lottery_entry_id")
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "performance_id", nullable = false)
	private Long performanceId;

	@Column(name = "schedule_id", nullable = false)
	private Long scheduleId;

	@Column(name = "seat_grade_id", nullable = false)
	private Long seatGradeId;

	@Column(name = "price", nullable = false)
	private Integer price;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private LotteryStatus status;

	@Builder
	public LotteryEntry(
		Long memberId,
		Long performanceId,
		Long scheduleId,
		Long seatGradeId,
		Integer price,
		Integer quantity
	) {
		this.memberId = memberId;
		this.performanceId = performanceId;
		this.scheduleId = scheduleId;
		this.seatGradeId = seatGradeId;
		this.price = price;
		this.quantity = quantity;
		this.status = LotteryStatus.APPLIED;
	}
}
