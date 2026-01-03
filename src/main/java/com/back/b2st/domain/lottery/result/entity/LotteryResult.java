package com.back.b2st.domain.lottery.result.entity;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import org.hibernate.annotations.DynamicUpdate;

import com.back.b2st.global.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
	name = "lottery_results",
	indexes = {
		@Index(name = "idx_lottery_results_lottery_entry", columnList = "lottery_entry_id"),
		@Index(name = "idx_lottery_results_member", columnList = "member_id"),
		@Index(name = "idx_lottery_results_lottery_entry_member", columnList = "lottery_entry_id, member_id"),
		@Index(name = "idx_lottery_results_payment_deadline", columnList = "payment_deadline"),
		@Index(name = "idx_lottery_results_uuid", columnList = "uuid"),
		@Index(name = "idx_lottery_results_paid", columnList = "is_paid")
	},
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_lottery_entry_member",
			columnNames = {"lottery_entry_id", "member_id"}
		),
		@UniqueConstraint(
			name = "uk_lottery_result_uuid",
			columnNames = {"uuid"}
		)
	}
)
@SequenceGenerator(
	name = "lottery_result_id_gen",
	sequenceName = "lottery_result_seq",
	allocationSize = 50
)
@DynamicUpdate
public class LotteryResult extends BaseEntity {

	public static final int PAYMENT_DEADLINE_DAYS = 2;    // TODO : 생성일 + 2일(임시)

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lottery_result_id_gen")
	@Column(name = "lottery_result_id")
	private Long id;

	@Column(name = "uuid", nullable = false, updatable = false, unique = true, columnDefinition = "uuid")
	private UUID uuid;

	@Column(name = "lottery_entry_id", nullable = false)
	private Long lotteryEntryId;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "payment_deadline", nullable = false)
	private LocalDateTime paymentDeadline;

	@Column(name = "is_paid", nullable = false)
	private boolean paid;

	@PrePersist
	public void generateUuid() {
		if (this.uuid == null) {
			this.uuid = UUID.randomUUID();
		}
	}

	@Builder
	public LotteryResult(
		Long lotteryEntryId,
		Long memberId
	) {
		this.lotteryEntryId = lotteryEntryId;
		this.memberId = memberId;
		this.paymentDeadline = LocalDateTime.now()
			.plusDays(PAYMENT_DEADLINE_DAYS).with(LocalTime.MAX);
		this.paid = false;
	}

	public void confirmPayment() {
		this.paid = true;
	}
}
