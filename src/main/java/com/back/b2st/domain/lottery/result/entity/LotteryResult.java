package com.back.b2st.domain.lottery.result.entity;

import java.time.LocalDateTime;

import com.back.b2st.global.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
	name = "lottery_results",
	indexes = {
		@Index(name = "idx_lottery_results_member", columnList = "member_id"),
		@Index(name = "idx_lottery_results_schedule", columnList = "schedule_id"),
		@Index(name = "idx_lottery_results_member_schedule", columnList = "member_id, schedule_id"),
		@Index(name = "idx_lottery_results_payment_deadline", columnList = "payment_deadline"),
		@Index(name = "idx_lottery_results_payment_deadline", columnList = "payment_deadline")
	}
)
@SequenceGenerator(
	name = "lottery_result_id_gen",
	sequenceName = "LOTTERY_RESULT_SEQ",
	allocationSize = 50
)
public class LotteryResult extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lottery_result_id_gen")
	@Column(name = "lottery_result_id")
	private Long id;

	@Column(name = "lottery_entry_id", nullable = false)
	private Long lotteryEntryId;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "seat_id", nullable = false)
	private Long seatId;

	@Column(name = "payment_deadline", nullable = false)
	private LocalDateTime paymentDeadline;

	@Column(name = "is_paid", nullable = false)
	private boolean paid = false;

	@Builder
	public LotteryResult(
		Long lotteryEntryId,
		Long memberId,
		Long seatId,
		LocalDateTime paymentDeadline
	) {
		this.lotteryEntryId = lotteryEntryId;
		this.memberId = memberId;
		this.seatId = seatId;
		this.paymentDeadline = LocalDateTime.now().plusDays(3);    // 생성일 + 3일
		this.paid = false;
	}
}
