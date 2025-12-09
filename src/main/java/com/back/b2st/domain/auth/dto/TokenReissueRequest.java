package com.back.b2st.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TokenReissueRequest {

	@NotBlank(message = "Access Token은 필수입니다.")
	private String accessToken;

	@NotBlank(message = "Refresh Token은 필수입니다.")
	private String refreshToken;
}
