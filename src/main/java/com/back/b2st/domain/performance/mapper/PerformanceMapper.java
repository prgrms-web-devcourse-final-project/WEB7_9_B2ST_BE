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
 * 핵심 원칙: DTO는 순수하게 유지하고, posterKey -> 최종 URL 변환은 여기서 수행
 * DB에는 posterKey만 저장하고, 응답에서 URL을 조립합니다.
 */
@Component
@RequiredArgsConstructor
public class PerformanceMapper {

	private final S3Service s3Service;

	/**
	 * Performance 엔티티를 PerformanceListRes로 변환
	 */
	public PerformanceListRes toListRes(Performance performance, LocalDateTime now) {
		String resolvedPosterUrl = resolvePosterUrl(performance.getPosterKey());
		return PerformanceListRes.from(performance, now, resolvedPosterUrl);
	}

	/**
	 * Performance 엔티티를 PerformanceDetailRes로 변환
	 */
	public PerformanceDetailRes toDetailRes(Performance performance, LocalDateTime now, List<PerformanceDetailRes.GradePrice> gradePrices) {
		String resolvedPosterUrl = resolvePosterUrl(performance.getPosterKey());
		return PerformanceDetailRes.from(performance, now, gradePrices, resolvedPosterUrl);
	}

	/**
	 * posterKey를 최종 URL로 변환
	 *
	 * Service 레이어에서 이미 정규화된 posterKey를 사용하므로,
	 * 여기서는 URL 조립만 수행합니다.
	 * 단, 다른 코드 경로(배치/관리자툴/테스트)에서 공백이 들어올 수 있으므로
	 * 방어적으로 trim()만 수행합니다.
	 * CloudFront 사용 여부는 S3Service 내부에서 처리합니다.
	 */
	private String resolvePosterUrl(String posterKey) {
		if (posterKey == null || posterKey.isBlank()) {
			return null;
		}

		// 정규화는 Service에서 수행했으므로, 방어적으로 trim()만 수행 후 URL 조립
		return s3Service.buildPublicUrl(posterKey.trim());
	}
}
