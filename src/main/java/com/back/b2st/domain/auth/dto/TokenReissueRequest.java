package com.back.b2st.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 지양하고 싶지만 빌더를 위해 private으로 막고 넣었습니다
@Builder
public class TokenReissueRequest {

	@NotBlank(message = "Access Token은 필수입니다.")
	private String accessToken;

	@NotBlank(message = "Refresh Token은 필수입니다.")
	private String refreshToken;
}
