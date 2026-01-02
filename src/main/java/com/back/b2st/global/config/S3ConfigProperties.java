package com.back.b2st.global.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * AWS S3 설정 Properties (Presigned URL 기반 업로드/서빙)
 *
 * - bucket: 필수
 * - cdnBaseUrl: 선택(CloudFront 도메인, 예: https://dxxxx.cloudfront.net)
 */
@Validated
@ConfigurationProperties(prefix = "aws.s3")
public record S3ConfigProperties(

	/** S3 버킷 이름 (필수) */
	@NotBlank(message = "aws.s3.bucket은(는) 필수입니다.")
	String bucket,

	/** AWS 리전 (기본: ap-northeast-2) */
	@NotBlank(message = "aws.s3.region은(는) 비어있을 수 없습니다.")
	String region,

	/** CloudFront CDN Base URL (선택) */
	String cdnBaseUrl,

	/** Presigned URL 만료 시간 (초, 기본: 300초 = 5분) */
	@Min(value = 1, message = "aws.s3.presign-expiration-seconds는 1 이상이어야 합니다.")
	int presignExpirationSeconds,

	/** 업로드 파일 크기 제한 (바이트, 기본: 10MB) */
	@Min(value = 1, message = "aws.s3.max-file-size는 1 이상이어야 합니다.")
	long maxFileSize,

	/** 허용된 Content-Type 목록 */
	@NotEmpty(message = "aws.s3.allowed-content-types는 비어있을 수 없습니다.")
	List<String> allowedContentTypes
) {
	public S3ConfigProperties {
		// 기본값 적용 (YAML 미설정 시)
		if (region == null || region.isBlank()) region = "ap-northeast-2";
		if (presignExpirationSeconds <= 0) presignExpirationSeconds = 300;
		if (maxFileSize <= 0) maxFileSize = 10 * 1024 * 1024;
		if (allowedContentTypes == null || allowedContentTypes.isEmpty()) {
			allowedContentTypes = List.of("image/jpeg", "image/png", "image/webp");
		}
	}
}
