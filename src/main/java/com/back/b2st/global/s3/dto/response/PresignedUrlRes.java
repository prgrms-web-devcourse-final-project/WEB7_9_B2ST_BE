package com.back.b2st.global.s3.dto.response;

/**
 * Presigned URL 응답 DTO
 */
public record PresignedUrlRes(
	/**
	 * S3 Object Key (DB에 저장할 값)
	 */
	String objectKey,

	/**
	 * Presigned PUT URL (클라이언트가 이 URL로 업로드)
	 */
	String uploadUrl,

	/**
	 * 만료 시간 (초)
	 */
	int expiresInSeconds,

	/**
	 * Public URL (조회 시 사용, CDN 또는 S3 직접 URL)
	 */
	String publicUrl
) {
}

