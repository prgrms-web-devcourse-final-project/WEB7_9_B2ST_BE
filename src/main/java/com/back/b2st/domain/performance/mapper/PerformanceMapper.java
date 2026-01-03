package com.back.b2st.domain.performance.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.back.b2st.domain.performance.dto.response.PerformanceDetailRes;
import com.back.b2st.domain.performance.dto.response.PerformanceListRes;
import com.back.b2st.domain.performance.entity.Performance;
import com.back.b2st.global.s3.service.S3Service;

import lombok.RequiredArgsConstructor;

/**
 * Performance 엔티티를 DTO로 변환하는 Mapper
 *
 * - S3는 Private 유지
 * - posterKey를 Presigned GET URL로 변환하여 응답에 포함
 */
@Component
@RequiredArgsConstructor
public class PerformanceMapper {

	private final S3Service s3Service;

	public PerformanceListRes toListRes(Performance performance, LocalDateTime now) {
		String posterUrl = resolvePosterUrl(performance.getPosterKey());
		return PerformanceListRes.from(performance, now, posterUrl);
	}

	public PerformanceDetailRes toDetailRes(
		Performance performance,
		LocalDateTime now,
		List<PerformanceDetailRes.GradePrice> gradePrices
	) {
		String posterUrl = resolvePosterUrl(performance.getPosterKey());
		return PerformanceDetailRes.from(performance, now, gradePrices, posterUrl);
	}

	/**
	 * posterKey를 Presigned GET URL로 변환
	 *
	 * - S3가 Private이므로 일반 URL 조립은 불가
	 * - 필요 시점마다 Presigned GET URL을 발급한다.
	 */
	private String resolvePosterUrl(String posterKey) {
		if (posterKey == null || posterKey.isBlank()) {
			return null;
		}

		return s3Service.generatePresignedDownloadUrl(posterKey.trim());
	}
}
