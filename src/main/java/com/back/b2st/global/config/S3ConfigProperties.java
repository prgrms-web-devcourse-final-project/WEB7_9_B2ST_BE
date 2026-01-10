package com.back.b2st.global.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * AWS S3 설정 Properties (Presigned URL 기반 업로드/다운로드)
 *
 * - S3는 Private 유지
 * - 업로드/조회 모두 Presigned URL로 처리
 */
@Validated
@ConfigurationProperties(prefix = "aws.s3")
public record S3ConfigProperties(

	/** S3 버킷 이름 (필수) */
	@NotBlank(message = "aws.s3.bucket은(는) 필수입니다.")
	String bucket,

	/**
	 * AWS 리전
	 * - 기본: ap-northeast-2
	 * - 가능하면 설정으로 명시하는 것을 권장
	 */
	String region,

	/** 업로드(PUT) Presigned URL 만료 시간 (초) */
	@Min(value = 1, message = "aws.s3.put-presign-expiration-seconds는 1 이상이어야 합니다.")
	int putPresignExpirationSeconds,

	/** 조회(GET) Presigned URL 만료 시간 (초) */
	@Min(value = 1, message = "aws.s3.get-presign-expiration-seconds는 1 이상이어야 합니다.")
	int getPresignExpirationSeconds,

	/** 업로드 파일 크기 제한 (바이트) */
	@Min(value = 1, message = "aws.s3.max-file-size는 1 이상이어야 합니다.")
	long maxFileSize,

	/** 허용된 Content-Type 목록 */
	@NotEmpty(message = "aws.s3.allowed-content-types는 비어있을 수 없습니다.")
	List<String> allowedContentTypes

) {
	public S3ConfigProperties {
		if (region == null || region.isBlank()) region = "ap-northeast-2";
		if (putPresignExpirationSeconds <= 0) putPresignExpirationSeconds = 300;
		if (getPresignExpirationSeconds <= 0) getPresignExpirationSeconds = 300;
		if (maxFileSize <= 0) maxFileSize = 10 * 1024 * 1024;

		if (allowedContentTypes == null || allowedContentTypes.isEmpty()) {
			allowedContentTypes = List.of("image/jpeg", "image/png", "image/webp");
		}
	}
}
