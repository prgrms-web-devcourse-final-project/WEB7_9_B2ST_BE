package com.back.b2st.domain.performance.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePresignedUrlReq(
	@NotBlank(message = "Content-Type은 필수입니다.")
	String contentType,

	@NotNull(message = "파일 크기는 필수입니다.")
	@Min(value = 1, message = "파일 크기는 1바이트 이상이어야 합니다.")
	@Max(value = 10485760, message = "파일 크기는 10MB 이하여야 합니다.")
	Long fileSize
) {
}

