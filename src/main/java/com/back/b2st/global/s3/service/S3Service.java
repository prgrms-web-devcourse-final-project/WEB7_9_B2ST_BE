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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

/**
 * AWS S3 서비스
 *
 * - Presigned PUT URL을 생성하여 클라이언트가 직접 S3에 업로드하도록 지원합니다.
 * - S3는 Private 유지: 조회는 Presigned GET URL로만 제공합니다.
 * - IAM Role 기반 인증을 사용하므로 Access Key/Secret Key를 코드에 저장하지 않습니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

	private final S3ConfigProperties s3Config;
	private final S3Presigner s3Presigner;

	/**
	 * 업로드용 Presigned URL 생성
	 *
	 * @param prefix S3 Object Key의 prefix (예: "performances/posters")
	 * @param contentType 파일의 Content-Type (image/jpeg, image/png, image/webp만 허용)
	 * @param fileSize 파일 크기 (바이트)
	 * @return PresignedUrlRes (objectKey, uploadUrl, expiresInSeconds)
	 */
	public PresignedUrlRes generatePresignedUploadUrl(String prefix, String contentType, long fileSize) {
		String normalizedPrefix = validateAndNormalizePrefix(prefix);
		String normalizedContentType = validateAndNormalizeContentType(contentType);

		if (fileSize <= 0 || fileSize > s3Config.maxFileSize()) {
			throw new BusinessException(
				CommonErrorCode.BAD_REQUEST,
				String.format("파일 크기는 1바이트 이상 %d바이트 이하여야 합니다.", s3Config.maxFileSize())
			);
		}

		String objectKey = generateObjectKey(normalizedPrefix, normalizedContentType);

		try {
			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(s3Config.bucket())
				.key(objectKey)
				.contentType(normalizedContentType)
				// .contentLength(fileSize) 제거: Presigned PUT에서 403 에러 유발 가능성
				.build();

			PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
				.signatureDuration(java.time.Duration.ofSeconds(s3Config.putPresignExpirationSeconds()))
				.putObjectRequest(putObjectRequest)
				.build();

			PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
			String uploadUrl = presignedRequest.url().toString();

			log.info("Presigned 업로드 URL 생성 완료 - ObjectKey: {}, ContentType: {}, FileSize: {} bytes",
				objectKey, normalizedContentType, fileSize);

			return new PresignedUrlRes(
				objectKey,
				uploadUrl,
				s3Config.putPresignExpirationSeconds()
			);
		} catch (Exception e) {
			log.error("Presigned 업로드 URL 생성 실패", e);
			throw new BusinessException(
				CommonErrorCode.INTERNAL_SERVER_ERROR,
				"Presigned 업로드 URL 생성에 실패했습니다: " + e.getMessage()
			);
		}
	}

	/**
	 * 조회용 Presigned GET URL 생성
	 *
	 * @param objectKey S3 Object Key
	 * @return 다운로드/조회 가능한 Presigned GET URL
	 */
	public String generatePresignedDownloadUrl(String objectKey) {
		String key = validateAndNormalizeObjectKey(objectKey);

		try {
			GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(s3Config.bucket())
				.key(key)
				.build();

			GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
				.signatureDuration(java.time.Duration.ofSeconds(s3Config.getPresignExpirationSeconds()))
				.getObjectRequest(getObjectRequest)
				.build();

			PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

			log.info("Presigned 다운로드 URL 생성 완료 - ObjectKey: {}", key);

			return presignedRequest.url().toString();
		} catch (Exception e) {
			log.error("Presigned 다운로드 URL 생성 실패", e);
			throw new BusinessException(
				CommonErrorCode.INTERNAL_SERVER_ERROR,
				"Presigned 다운로드 URL 생성에 실패했습니다: " + e.getMessage()
			);
		}
	}

	private String validateAndNormalizePrefix(String prefix) {
		if (prefix == null) {
			throw new BusinessException(CommonErrorCode.BAD_REQUEST, "prefix는 필수입니다.");
		}

		String s = prefix.trim();
		if (s.isEmpty()) {
			throw new BusinessException(CommonErrorCode.BAD_REQUEST, "prefix는 필수입니다.");
		}

		int start = 0;
		int end = s.length();

		while (start < end && s.charAt(start) == '/') start++;
		while (start < end && s.charAt(end - 1) == '/') end--;

		if (start >= end) {
			throw new BusinessException(CommonErrorCode.BAD_REQUEST, "prefix는 필수입니다.");
		}

		StringBuilder sb = new StringBuilder(end - start);
		boolean prevSlash = false;

		for (int i = start; i < end; i++) {
			char ch = s.charAt(i);
			if (ch == '/') {
				if (prevSlash) continue;
				prevSlash = true;
			} else {
				prevSlash = false;
			}
			sb.append(ch);
		}

		if (sb.length() == 0) {
			throw new BusinessException(CommonErrorCode.BAD_REQUEST, "prefix는 필수입니다.");
		}

		return sb.toString();
	}

	private String validateAndNormalizeContentType(String contentType) {
		if (contentType == null || contentType.trim().isEmpty()) {
			throw new BusinessException(CommonErrorCode.BAD_REQUEST, "Content-Type은 필수입니다.");
		}

		String normalized = contentType.toLowerCase(Locale.ROOT).trim();

		int semicolonIdx = normalized.indexOf(';');
		if (semicolonIdx > -1) {
			normalized = normalized.substring(0, semicolonIdx).trim();
		}

		if ("image/jpg".equals(normalized)) {
			normalized = "image/jpeg";
		}

		if (!s3Config.allowedContentTypes().contains(normalized)) {
			throw new BusinessException(
				CommonErrorCode.BAD_REQUEST,
				String.format("허용된 이미지 형식은 %s입니다.", String.join(", ", s3Config.allowedContentTypes()))
			);
		}

		return normalized;
	}

	private String validateAndNormalizeObjectKey(String objectKey) {
		if (objectKey == null || objectKey.trim().isEmpty()) {
			throw new BusinessException(CommonErrorCode.BAD_REQUEST, "objectKey는 필수입니다.");
		}

		String key = objectKey.trim();
		while (key.startsWith("/")) {
			key = key.substring(1);
		}

		if (key.isEmpty()) {
			throw new BusinessException(CommonErrorCode.BAD_REQUEST, "objectKey는 필수입니다.");
		}

		return key;
	}

	private String generateObjectKey(String normalizedPrefix, String normalizedContentType) {
		LocalDate now = LocalDate.now();
		String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
		String uuid = UUID.randomUUID().toString();
		String extension = getExtensionFromContentType(normalizedContentType);

		return String.format("%s/%s/%s.%s", normalizedPrefix, datePath, uuid, extension);
	}

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
}
