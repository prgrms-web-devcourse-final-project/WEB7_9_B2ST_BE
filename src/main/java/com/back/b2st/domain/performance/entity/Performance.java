package com.back.b2st.domain.performance.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import com.back.b2st.domain.performance.error.PerformanceErrorCode;
import com.back.b2st.domain.venue.venue.entity.Venue;
import com.back.b2st.global.error.exception.BusinessException;
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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "performance")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
	name = "performance_id_gen",
	sequenceName = "performance_seq",
	allocationSize = 50
)
public class Performance extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "performance_id_gen")
	@Column(name = "performance_id")
	private Long performanceId; // PK

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "venue_id", nullable = false)
	private Venue venue; // 공연장 FK

	@Column(nullable = false, length = 200)
	private String title; // 공연제목

	@Column(nullable = false, length = 50)
	private String category; // 장르

	@Column(name = "poster_key", length = 500)
	private String posterKey; // 포스터 이미지 S3 Object Key

	@Lob
	private String description; // 공연 설명

	@Column(name = "start_date", nullable = false)
	private LocalDateTime startDate; // 공연 시작일

	@Column(name = "end_date", nullable = false)
	private LocalDateTime endDate; // 공연 종료일

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PerformanceStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "booking_type")
	private BookingType bookingType; // 예매 유형 (추첨/일반예매/구역별 사전등록)

	@Column(name = "booking_open_at")
	private LocalDateTime bookingOpenAt; // 예매 오픈 시각 (null이면 예매 불가)

	@Column(name = "booking_close_at")
	private LocalDateTime bookingCloseAt; // 예매 마감 시각

	@Builder
	public Performance(
		Venue venue,
		String title,
		String category,
		String posterKey,
		String description,
		LocalDateTime startDate,
		LocalDateTime endDate,
		PerformanceStatus status,
		BookingType bookingType,
		LocalDateTime bookingOpenAt,
		LocalDateTime bookingCloseAt
	) {
		this.venue = Objects.requireNonNull(venue, "venue must not be null");
		this.title = Objects.requireNonNull(title, "title must not be null");
		this.category = Objects.requireNonNull(category, "category must not be null");
		this.posterKey = posterKey;
		this.description = description;
		this.startDate = Objects.requireNonNull(startDate, "startDate must not be null");
		this.endDate = Objects.requireNonNull(endDate, "endDate must not be null");
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.bookingType = bookingType;
		this.bookingOpenAt = bookingOpenAt;
		this.bookingCloseAt = bookingCloseAt;

		validatePeriod(this.startDate, this.endDate);
		validateBookingPolicy(this.bookingOpenAt, this.bookingCloseAt);
	}

	/**
	 * 공연 상태 변경
	 */
	public void updateStatus(PerformanceStatus status) {
		this.status = Objects.requireNonNull(status, "status must not be null");
	}

	/**
	 * 활성(노출) 여부.
	 * 주의: "예매 가능"은 isBookable(now)로 판단한다.
	 */
	public boolean isOnSale() {
		return this.status == PerformanceStatus.ACTIVE;
	}

	/**
	 * 예매 가능 여부 계산
	 * @param now 현재 시각
	 * @return 예매 가능 여부
	 */
	public boolean isBookable(LocalDateTime now) {
		Objects.requireNonNull(now, "now must not be null");

		if (status == PerformanceStatus.ENDED) return false;
		if (bookingOpenAt == null) return false;
		if (now.isBefore(bookingOpenAt)) return false;
		if (bookingCloseAt != null && !now.isBefore(bookingCloseAt)) return false; // now >= closeAt
		return true;
	}

	/**
	 * 예매 정책
	 * - bookingOpenAt은 null 허용(미설정 상태를 표현). 단, closeAt 단독 설정은 불가.
	 * - closeAt이 있으면 openAt < closeAt 이어야 함.
	 */
	public void updateBookingPolicy(LocalDateTime bookingOpenAt, LocalDateTime bookingCloseAt) {
		validateBookingPolicy(bookingOpenAt, bookingCloseAt);
		this.bookingOpenAt = bookingOpenAt;
		this.bookingCloseAt = bookingCloseAt;
	}

	@PrePersist
	@PreUpdate
	private void validateEntityState() {
		// JPA flush 시점에서도 최소 무결성 방어
		validatePeriod(this.startDate, this.endDate);
		validateBookingPolicy(this.bookingOpenAt, this.bookingCloseAt);
		if (this.status == null) {
			throw new BusinessException(PerformanceErrorCode.PERFORMANCE_INTERNAL_ERROR, "공연 상태는 필수입니다.");
		}
		if (this.venue == null) {
			throw new BusinessException(PerformanceErrorCode.PERFORMANCE_INTERNAL_ERROR, "공연장은 필수입니다.");
		}
	}

	private static void validatePeriod(LocalDateTime startDate, LocalDateTime endDate) {
		if (!startDate.isBefore(endDate)) {
			throw new BusinessException(PerformanceErrorCode.INVALID_PERFORMANCE_PERIOD);
		}
	}

	private static void validateBookingPolicy(LocalDateTime bookingOpenAt, LocalDateTime bookingCloseAt) {
		// closeAt만 단독 설정 금지
		if (bookingOpenAt == null && bookingCloseAt != null) {
			throw new BusinessException(PerformanceErrorCode.INVALID_BOOKING_POLICY);
		}
		// 둘 다 있으면 open < close
		if (bookingOpenAt != null && bookingCloseAt != null && !bookingOpenAt.isBefore(bookingCloseAt)) {
			throw new BusinessException(PerformanceErrorCode.INVALID_BOOKING_TIME);
		}
	}
}