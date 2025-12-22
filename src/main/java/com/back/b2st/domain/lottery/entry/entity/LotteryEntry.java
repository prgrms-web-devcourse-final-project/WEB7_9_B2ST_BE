package com.back.b2st.domain.lottery.entry.entity;

import java.util.UUID;

import org.hibernate.annotations.DynamicUpdate;

import com.back.b2st.domain.seat.grade.entity.SeatGradeType;
import com.back.b2st.global.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
		@Index(name = "idx_lottery_entries_member_created", columnList = "member_id, created_at"),
		@Index(name = "idx_lottery_entries_performance", columnList = "performance_id, schedule_id"),
		@Index(name = "idx_lottery_entries_schedule", columnList = "schedule_id"),
		@Index(name = "idx_lottery_entries_member_performance_schedule", columnList = "member_id, performance_id, schedule_id"),
		@Index(name = "idx_lottery_entries_status", columnList = "status"),
		@Index(name = "idx_lottery_entries_uuid", columnList = "uuid")
	},
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_member_performance_schedule",
			columnNames = {"member_id", "performance_id", "schedule_id"}
		),
		@UniqueConstraint(
			name = "uk_lottery_entry_uuid",
			columnNames = {"uuid"}
		)
	}
)
@SequenceGenerator(
	name = "lottery_entry_id_gen",
	sequenceName = "lottery_entry_seq",
	allocationSize = 50
)
@DynamicUpdate
public class LotteryEntry extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lottery_entry_id_gen")
	@Column(name = "lottery_entry_id")
	private Long id;

	@Column(name = "uuid", nullable = false, updatable = false, unique = true, columnDefinition = "uuid")
	private UUID uuid;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "performance_id", nullable = false)
	private Long performanceId;

	@Column(name = "schedule_id", nullable = false)
	private Long scheduleId;

	@Enumerated(EnumType.STRING)
	@Column(name = "grade", nullable = false, length = 20)
	private SeatGradeType grade;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private LotteryStatus status;

	@PrePersist
	public void generateUuid() {
		if (this.uuid == null) {
			this.uuid = UUID.randomUUID();
		}
	}

	@Builder
	public LotteryEntry(
		Long memberId,
		Long performanceId,
		Long scheduleId,
		SeatGradeType grade,
		Integer quantity
	) {
		this.memberId = memberId;
		this.performanceId = performanceId;
		this.scheduleId = scheduleId;
		this.grade = grade;
		this.quantity = quantity;
		this.status = LotteryStatus.APPLIED;
	}
}
