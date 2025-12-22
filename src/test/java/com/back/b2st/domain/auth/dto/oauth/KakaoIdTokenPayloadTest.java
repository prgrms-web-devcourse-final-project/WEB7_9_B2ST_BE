package com.back.b2st.domain.auth.dto.oauth;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("KakaoIdTokenPayload 테스트")
class KakaoIdTokenPayloadTest {

	private KakaoIdTokenPayload createPayload(String sub, String email, String nickname) {
		long now = System.currentTimeMillis() / 1000;
		return new KakaoIdTokenPayload(
			"https://kauth.kakao.com", // iss
			"test-client-id", // aud
			sub,
			now, // iat
			now + 3600, // exp
			now, // auth_time
			null, // nonce
			nickname,
			null, // picture
			email);
	}

	@Nested
	@DisplayName("getKakaoId")
	class GetKakaoIdTest {

		@Test
		@DisplayName("sub 값을 Long으로 변환")
		void success() {
			// given
			KakaoIdTokenPayload payload = createPayload("123456789", "test@kakao.com", "유저");

			// when
			Long kakaoId = payload.getKakaoId();

			// then
			assertThat(kakaoId).isEqualTo(123456789L);
		}

		@Test
		@DisplayName("sub이 null이면 null 반환")
		void returnNullWhenSubIsNull() {
			// given
			KakaoIdTokenPayload payload = new KakaoIdTokenPayload(
				"iss", "aud", null, 0L, 0L, 0L, null, "nickname", null, "email@test.com");

			// when
			Long kakaoId = payload.getKakaoId();

			// then
			assertThat(kakaoId).isNull();
		}
	}

	@Nested
	@DisplayName("hasEmail")
	class HasEmailTest {

		@Test
		@DisplayName("이메일이 있으면 true")
		void returnTrueWhenEmailExists() {
			// given
			KakaoIdTokenPayload payload = createPayload("123", "test@kakao.com", "유저");

			// when
			boolean result = payload.hasEmail();

			// then
			assertThat(result).isTrue();
		}

		@Test
		@DisplayName("이메일이 null이면 false")
		void returnFalseWhenEmailIsNull() {
			// given
			KakaoIdTokenPayload payload = createPayload("123", null, "유저");

			// when
			boolean result = payload.hasEmail();

			// then
			assertThat(result).isFalse();
		}

		@Test
		@DisplayName("이메일이 빈 문자열이면 false")
		void returnFalseWhenEmailIsBlank() {
			// given
			KakaoIdTokenPayload payload = createPayload("123", "   ", "유저");

			// when
			boolean result = payload.hasEmail();

			// then
			assertThat(result).isFalse();
		}
	}
}
