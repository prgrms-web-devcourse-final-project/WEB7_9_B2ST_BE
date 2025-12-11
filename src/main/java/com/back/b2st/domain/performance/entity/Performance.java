package com.back.b2st.domain.performance.entity;

import java.time.LocalDateTime;

import com.back.b2st.domain.venue.entity.Venue;
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
	private Long performanceId;	// PK

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "venue_id", nullable = false)
	private Venue venue;	//공연장 FK

	@Column(nullable = false, length = 200)
	private String title;	//

	@Column(nullable = false, length = 50)
	private String category;	//장르

	@Column(name = "poster_url", length = 500)
	private String posterUrl;	//포스터 이미지 URL

	@Lob
	private String description;	//공연 설명

	@Column(name = "start_date", nullable = false)
	private LocalDateTime startDate;	//공연 시작일

	@Column(name = "end_date", nullable = false)
	private LocalDateTime endDate; //공연 종료일

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PerformanceStatus status;

	@Builder
	public Performance(
			Venue venue,
			String title,
			String category,
			String posterUrl,
			String description,
			LocalDateTime startDate,
			LocalDateTime endDate,
			PerformanceStatus status
	) {
		this.venue = venue;
		this.title = title;
		this.category = category;
		this.posterUrl = posterUrl;
		this.description = description;
		this.startDate = startDate;
		this.endDate = endDate;
		this.status = status;
	}

	/** 공연 상태 변경 */
	public void updateStatus(PerformanceStatus status) {
		this.status = status;
	}

	/** 공연이 판매 중인지 여부 */
	public boolean isOnSale() {
		return this.status == PerformanceStatus.ON_SALE;
	}
}

