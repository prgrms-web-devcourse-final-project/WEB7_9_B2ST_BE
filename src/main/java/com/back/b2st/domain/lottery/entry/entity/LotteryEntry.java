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
		@Index(name = "idx_lottery_entries_schedule", columnList = "schedule_id"),
		@Index(name = "idx_lottery_entries_member_schedule", columnList = "member_id, schedule_id"),
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
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lotter_entrys_id_gen")
	@Column(name = "lotter_entrys_id")
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "schedule_id", nullable = false)
	private Long scheduleId;

	// @Enumerated(EnumType.STRING)
	@Column(name = "grade", nullable = false, length = 20)
	private String grade;    // TODO Long? Enum?

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
		Long scheduleId,
		String grade,
		Integer price,
		Integer quantity
	) {
		this.memberId = memberId;
		this.scheduleId = scheduleId;
		this.grade = grade;
		this.price = price;
		this.quantity = quantity;
		this.status = LotteryStatus.APPLIED;
	}
}
