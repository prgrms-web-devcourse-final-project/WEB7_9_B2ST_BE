package com.back.b2st.global.s3.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.back.b2st.global.config.S3ConfigProperties;
import com.back.b2st.global.error.code.CommonErrorCode;
import com.back.b2st.global.error.exception.BusinessException;
import com.back.b2st.global.s3.dto.response.PresignedUrlRes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

/**
 * AWS S3 서비스
 *
 * Presigned URL을 생성하여 클라이언트가 직접 S3에 업로드할 수 있도록 합니다.
 * IAM Role 기반 인증을 사용하므로 Access Key/Secret Key를 코드에 저장하지 않습니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

	private final S3ConfigProperties s3Config;
	private final S3Presigner s3Presigner;

	/**
	 * Presigned URL 생성
	 *
	 * @param prefix S3 Object Key의 prefix (예: "performances/posters")
	 * @param contentType 파일의 Content-Type (image/jpeg, image/png, image/webp만 허용)
	 * @param fileSize 파일 크기 (바이트)
	 * @return PresignedUrlRes (objectKey, uploadUrl, expiresInSeconds, publicUrl)
	 */
	public PresignedUrlRes generatePresignedUrl(String prefix, String contentType, long fileSize) {
		// prefix 검증 및 정규화
		String normalizedPrefix = validateAndNormalizePrefix(prefix);

		// Content-Type 검증 및 정규화 (정규화된 값을 presign 서명에 사용)
		String normalizedContentType = validateAndNormalizeContentType(contentType);

		// 파일 크기 검증
		if (fileSize <= 0 || fileSize > s3Config.maxFileSize()) {
			throw new BusinessException(
				CommonErrorCode.BAD_REQUEST,
				String.format("파일 크기는 1바이트 이상 %d바이트 이하여야 합니다.", s3Config.maxFileSize())
			);
		}

		// Object Key 생성 (서버에서 생성하여 보안 강화)
		String objectKey = generateObjectKey(normalizedPrefix, normalizedContentType);

		// Presigned URL 생성
		try {
			// contentLength는 Presigned PUT에서 403 에러를 유발할 수 있으므로 서명에 포함하지 않음
			// 브라우저/프록시 환경에서 Content-Length 처리 방식 차이로 인한 간헐적 실패 방지
			// 정규화된 contentType을 사용하여 서명과 실제 업로드 시 헤더가 일치하도록 함
			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(s3Config.bucket())
				.key(objectKey)
				.contentType(normalizedContentType) // 정규화된 값 사용
				// .contentLength(fileSize) 제거: Presigned PUT에서 403 에러 유발 가능성
				.build();

			PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
				.signatureDuration(java.time.Duration.ofSeconds(s3Config.presignExpirationSeconds()))
				.putObjectRequest(putObjectRequest)
				.build();

			PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
			String uploadUrl = presignedRequest.url().toString();

			// Public URL 생성 (CDN이 있으면 CDN 사용, 없으면 S3 직접 URL)
			String publicUrl = buildPublicUrl(objectKey);

			log.info("Presigned URL 생성 완료 - ObjectKey: {}, ContentType: {}, FileSize: {} bytes",
				objectKey, normalizedContentType, fileSize);

			return new PresignedUrlRes(
				objectKey,
				uploadUrl,
				s3Config.presignExpirationSeconds(),
				publicUrl
			);
		} catch (Exception e) {
			log.error("Presigned URL 생성 실패", e);
			throw new BusinessException(
				CommonErrorCode.INTERNAL_SERVER_ERROR,
				"Presigned URL 생성에 실패했습니다: " + e.getMessage()
			);
		}
	}

	/**
	 * prefix 검증 및 정규화
	 *
	 * @param prefix S3 Object Key의 prefix
	 * @return 정규화된 prefix
	 * @throws BusinessException prefix가 null이거나 빈 값인 경우
	 */
	private String validateAndNormalizePrefix(String prefix) {
		if (prefix == null || prefix.trim().isEmpty()) {
			throw new BusinessException(
				CommonErrorCode.BAD_REQUEST,
				"prefix는 필수입니다."
			);
		}

		String normalized = prefix.trim();

		// 선행 슬래시 제거 (여러 개)
		while (normalized.startsWith("/")) {
			normalized = normalized.substring(1);
		}

		// 후행 슬래시 제거 (여러 개)
		while (normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}

		// 중간 연속 슬래시 정규화 (예: "performances//posters" -> "performances/posters")
		// CloudFront 캐시 키/경로가 꼬이지 않도록 방지
		// 한 번에 처리하여 성능/가독성 향상
		normalized = normalized.replaceAll("/{2,}", "/");

		// 정규화 후에도 빈 값인지 확인
		if (normalized.isEmpty()) {
			throw new BusinessException(
				CommonErrorCode.BAD_REQUEST,
				"prefix는 필수입니다."
			);
		}

		return normalized;
	}

	/**
	 * Content-Type 검증 및 정규화
	 *
	 * @param contentType 파일의 Content-Type
	 * @return 정규화된 Content-Type (소문자, 공백 제거)
	 * @throws BusinessException Content-Type이 유효하지 않은 경우
	 */
	private String validateAndNormalizeContentType(String contentType) {
		if (contentType == null || contentType.trim().isEmpty()) {
			throw new BusinessException(
				CommonErrorCode.BAD_REQUEST,
				"Content-Type은 필수입니다."
			);
		}

		// 대소문자/공백 normalize
		String normalizedContentType = contentType.toLowerCase(Locale.ROOT).trim();

		// 설정값 기반 검증 (정규식 패턴 대신 설정값 사용)
		if (!s3Config.allowedContentTypes().contains(normalizedContentType)) {
			throw new BusinessException(
				CommonErrorCode.BAD_REQUEST,
				String.format("허용된 이미지 형식은 %s입니다.", String.join(", ", s3Config.allowedContentTypes()))
			);
		}

		return normalizedContentType;
	}

	/**
	 * Object Key 생성
	 * 형식: {prefix}/{yyyy}/{mm}/{dd}/{uuid}.{extension}
	 *
	 * @param normalizedPrefix 정규화된 prefix
	 * @param normalizedContentType 정규화된 Content-Type
	 */
	private String generateObjectKey(String normalizedPrefix, String normalizedContentType) {
		LocalDate now = LocalDate.now();
		String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
		String uuid = UUID.randomUUID().toString();

		// Content-Type에서 확장자 결정
		String extension = getExtensionFromContentType(normalizedContentType);

		return String.format("%s/%s/%s.%s", normalizedPrefix, datePath, uuid, extension);
	}

	/**
	 * Content-Type에서 확장자 추출
	 *
	 * @param normalizedContentType 정규화된 Content-Type (소문자)
	 * @return 파일 확장자
	 * @throws BusinessException 허용되지 않은 Content-Type인 경우 (이론상 도달 불가능)
	 */
	private String getExtensionFromContentType(String normalizedContentType) {
		return switch (normalizedContentType) {
			case "image/jpeg" -> "jpg";
			case "image/png" -> "png";
			case "image/webp" -> "webp";
			default -> throw new BusinessException(
				CommonErrorCode.INTERNAL_SERVER_ERROR,
				"지원하지 않는 Content-Type입니다: " + normalizedContentType
			);
		};
	}


	/**
	 * Public URL 생성 (CDN이 있으면 CDN 사용, 없으면 S3 직접 URL)
	 */
	public String buildPublicUrl(String objectKey) {
		if (s3Config.cdnBaseUrl() != null && !s3Config.cdnBaseUrl().trim().isEmpty()) {
			// CDN URL 사용
			String baseUrl = s3Config.cdnBaseUrl().trim();
			if (baseUrl.endsWith("/")) {
				return baseUrl + objectKey;
			}
			return baseUrl + "/" + objectKey;
		} else {
			// S3 직접 URL
			return String.format("https://%s.s3.%s.amazonaws.com/%s",
				s3Config.bucket(),
				s3Config.region(),
				objectKey);
		}
	}
}

