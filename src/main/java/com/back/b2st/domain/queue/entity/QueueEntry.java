package com.back.b2st.domain.queue.entity;

import java.time.LocalDateTime;
import java.util.UUID;

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
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대기열 입장 기록 엔티티
 *
 * 인덱스 전략:
 * - UNIQUE 제약이 자동으로 인덱스 생성 (uk_queue_user, uk_entry_token)
 * - cleanup 쿼리용: (status, expires_at)
 * - 통계 쿼리용: (queue_id, status)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "queue_entries",
	indexes = {
		// cleanup 쿼리용 (필수)
		@Index(name = "idx_queue_entries_status_expires",
			columnList = "status, expires_at"),

		// 통계 쿼리용 (권장)
		@Index(name = "idx_queue_entries_queue_status",
			columnList = "queue_id, status")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_queue_user",
			columnNames = {"queue_id", "user_id"}),
		@UniqueConstraint(name = "uk_entry_token",
			columnNames = {"entry_token"})
	}
)
@SequenceGenerator(
	name = "queue_entry_id_gen",
	sequenceName = "queue_entry_seq",
	allocationSize = 50
)
@DynamicUpdate
public class QueueEntry extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "queue_entry_id_gen")
	@Column(name = "queue_entry_id")
	@Schema(description = "대기열 참가 ID", example = "1")
	private Long id;

	@Column(name = "entry_token", nullable = false, columnDefinition = "uuid")
	@Schema(description = "입장권 토큰 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
	private UUID entryToken;

	@Column(name = "queue_id", nullable = false)
	@Schema(description = "대기열 ID", example = "1")
	private Long queueId;

	@Column(name = "user_id", nullable = false)
	@Schema(description = "사용자 ID", example = "100")
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	@Schema(description = "참가 상태", example = "ENTERABLE")
	private QueueEntryStatus status;

	@Column(name = "joined_at")
	@Schema(description = "대기 시작 시각", example = "2024-12-26T14:00:00")
	private LocalDateTime joinedAt;

	@Column(name = "enterable_at")
	@Schema(description = "입장 가능 시각", example = "2024-12-26T14:05:00")
	private LocalDateTime enterableAt;

	@Column(name = "expires_at")
	@Schema(description = "입장 만료 시각", example = "2024-12-26T14:15:00")
	private LocalDateTime expiresAt;

	@Column(name = "completed_at")
	@Schema(description = "최종 완료 시각", example = "2024-12-26T14:10:00")
	private LocalDateTime completedAt;

	@PrePersist
	public void generateEntryToken() {
		if (this.entryToken == null) {
			this.entryToken = UUID.randomUUID();
		}
	}

	@Builder
	public QueueEntry(
		Long queueId,
		Long userId,
		UUID entryToken,
		QueueEntryStatus status,
		LocalDateTime joinedAt,
		LocalDateTime enterableAt,
		LocalDateTime expiresAt
	) {
		this.queueId = queueId;
		this.userId = userId;
		this.entryToken = entryToken;
		this.status = (status != null) ? status : QueueEntryStatus.EXPIRED;
		this.joinedAt = joinedAt;
		this.enterableAt = enterableAt;
		this.expiresAt = expiresAt;
	}

	// ===== 상태 전이 메서드 =====

	public void updateToEnterable(
		UUID newEntryToken,
		LocalDateTime joinedAt,
		LocalDateTime enterableAt,
		LocalDateTime expiresAt
	) {
		this.entryToken = newEntryToken;
		this.status = QueueEntryStatus.ENTERABLE;
		this.joinedAt = joinedAt;
		this.enterableAt = enterableAt;
		this.expiresAt = expiresAt;
		this.completedAt = null;
	}

	public void updateToExpired() {
		this.status = QueueEntryStatus.EXPIRED;
	}

	public void updateToCompleted(LocalDateTime completedAt) {
		this.status = QueueEntryStatus.COMPLETED;
		this.completedAt = completedAt;
	}
}