package com.back.b2st.domain.queue.entity;

import org.hibernate.annotations.DynamicUpdate;

import com.back.b2st.global.jpa.entity.BaseEntity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대기열 설정 엔티티
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "queues",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_queue_performance",
			columnNames = {"performance_id"}
		)
	}
)
@SequenceGenerator(
	name = "queue_id_gen",
	sequenceName = "queue_seq",
	allocationSize = 50
)
@DynamicUpdate
public class Queue extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "queue_id_gen")
	@Column(name = "queue_id")
	@Schema(description = "대기열 ID", example = "1")
	private Long id;

	@Column(name = "performance_id", nullable = false)
	@Schema(description = "공연 ID", example = "1")
	private Long performanceId;

	@Enumerated(EnumType.STRING)
	@Column(name = "queue_type", nullable = false, length = 20)
	@Schema(description = "대기열 타입", example = "BOOKING_ORDER")
	private QueueType queueType;

	@Column(name = "max_active_users", nullable = false)
	@Schema(description = "동시 입장 허용 수", example = "200")
	private Integer maxActiveUsers;

	@Column(name = "entry_ttl_minutes", nullable = false)
	@Schema(description = "입장권 유효 시간(분)", example = "10")
	private Integer entryTtlMinutes;

	@Builder
	public Queue(
		Long performanceId,
		QueueType queueType,
		Integer maxActiveUsers,
		Integer entryTtlMinutes
	) {
		this.performanceId = performanceId;
		this.queueType = queueType;
		this.maxActiveUsers = maxActiveUsers;
		this.entryTtlMinutes = entryTtlMinutes;
	}

	// 편의 메서드
	public void updateMaxActiveUsers(Integer maxActiveUsers) {
		if (maxActiveUsers <= 0) {
			throw new IllegalArgumentException("maxActiveUsers must be positive");
		}
		this.maxActiveUsers = maxActiveUsers;
	}

	public void updateEntryTtl(Integer entryTtlMinutes) {
		if (entryTtlMinutes <= 0) {
			throw new IllegalArgumentException("entryTtlMinutes must be positive");
		}
		this.entryTtlMinutes = entryTtlMinutes;
	}
}

