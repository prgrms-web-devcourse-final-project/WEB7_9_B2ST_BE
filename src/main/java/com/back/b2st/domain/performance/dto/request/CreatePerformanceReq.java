package com.back.b2st.domain.performance.dto.request;

import java.time.LocalDateTime;

import com.back.b2st.domain.performance.entity.BookingType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 공연 생성 요청 DTO
 */
public record CreatePerformanceReq(
	@NotNull
	Long venueId,

	@NotBlank
	@Size(max = 200)
	String title,

	@NotBlank
	@Size(max = 50)
	String category,

	/**
	 * 포스터 이미지의 S3 objectKey (DB 저장 값).
	 * 클라이언트는 presigned URL 업로드 후 반환받은 objectKey를 posterKey로 전달합니다.
	 * DB에는 posterKey(objectKey)만 저장하고, 응답 DTO에서는 이를 public URL로 변환해 내려줍니다.
	 */
	@Size(max = 500)
	String posterKey,

	@Size(max = 5000)
	String description,

	@NotNull
	LocalDateTime startDate,

	@NotNull
	LocalDateTime endDate,

	BookingType bookingType,  // 추가 (nullable, 기존 코드 호환성 고려)

	LocalDateTime bookingOpenAt,  // 기존에 있었을 수도 있는 필드들

	LocalDateTime bookingCloseAt
) {
}