package com.back.b2st.domain.auth.client;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.back.b2st.domain.auth.dto.oauth.KakaoTokenRes;
import com.back.b2st.domain.auth.error.AuthErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("KakaoApiClientImpl 테스트")
class KakaoApiClientImplTest {

	private static final String CLIENT_ID = "test-client-id";
	private static final String CLIENT_SECRET = "test-client-secret";
	private static final String REDIRECT_URI = "http://localhost:8080/api/auth/kakao/callback";
	private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
	private static final String ISSUER = "https://kauth.kakao.com";
	private KakaoApiClientImpl kakaoApiClient;
	@Mock
	private RestTemplate restTemplate;
	@Mock
	private KakaoJwksClient jwksClient;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		kakaoApiClient = new KakaoApiClientImpl(restTemplate, objectMapper, jwksClient);

		// 설정값 주입
		ReflectionTestUtils.setField(kakaoApiClient, "clientId", CLIENT_ID);
		ReflectionTestUtils.setField(kakaoApiClient, "clientSecret", CLIENT_SECRET);
		ReflectionTestUtils.setField(kakaoApiClient, "redirectUri", REDIRECT_URI);
		ReflectionTestUtils.setField(kakaoApiClient, "tokenUri", TOKEN_URI);
		ReflectionTestUtils.setField(kakaoApiClient, "issuer", ISSUER);
	}

	@Nested
	@DisplayName("토큰 발급 실패 케이스")
	class TokenIssuanceFailTest {

		@Test
		@DisplayName("실패 - 토큰 응답이 null")
		void fail_nullResponse() {
			// given
			String code = "test-code";

			given(restTemplate.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(KakaoTokenRes.class)))
				.willReturn(ResponseEntity.ok(null));

			// when & then
			assertThatThrownBy(() -> kakaoApiClient.getTokenAndParseIdToken(code))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
		}

		@Test
		@DisplayName("실패 - 네트워크 오류")
		void fail_networkError() {
			// given
			String code = "test-code";

			given(restTemplate.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(KakaoTokenRes.class)))
				.willThrow(new RestClientException("Connection refused"));

			// when & then
			assertThatThrownBy(() -> kakaoApiClient.getTokenAndParseIdToken(code))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
		}

		@Test
		@DisplayName("실패 - id_token이 없음 (scope에 openid 미포함)")
		void fail_noIdToken() {
			// given
			String code = "test-code";

			KakaoTokenRes tokenRes = new KakaoTokenRes(
				"bearer", "access-token", 21599,
				"refresh-token", 5183999, "profile_nickname account_email",
				null // id_token 없음
			);

			given(restTemplate.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(KakaoTokenRes.class)))
				.willReturn(ResponseEntity.ok(tokenRes));

			// when & then
			assertThatThrownBy(() -> kakaoApiClient.getTokenAndParseIdToken(code))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
		}

		@Test
		@DisplayName("실패 - id_token이 빈 문자열")
		void fail_emptyIdToken() {
			// given
			String code = "test-code";

			KakaoTokenRes tokenRes = new KakaoTokenRes(
				"bearer", "access-token", 21599,
				"refresh-token", 5183999, "openid",
				"" // 빈 문자열
			);

			given(restTemplate.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(KakaoTokenRes.class)))
				.willReturn(ResponseEntity.ok(tokenRes));

			// when & then
			assertThatThrownBy(() -> kakaoApiClient.getTokenAndParseIdToken(code))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
		}
	}

	@Nested
	@DisplayName("id_token 파싱 실패 케이스")
	class IdTokenParsingFailTest {

		@Test
		@DisplayName("실패 - 잘못된 JWT 형식")
		void fail_malformedToken() {
			// given
			String code = "test-code";
			String malformedToken = "not.a.valid.jwt.token";

			KakaoTokenRes tokenRes = new KakaoTokenRes(
				"bearer", "access-token", 21599,
				null, 0, "openid",
				malformedToken);

			given(restTemplate.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(KakaoTokenRes.class)))
				.willReturn(ResponseEntity.ok(tokenRes));

			// when & then
			assertThatThrownBy(() -> kakaoApiClient.getTokenAndParseIdToken(code))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
		}

		@Test
		@DisplayName("실패 - JWT 형식이 아닌 문자열")
		void fail_notJwtFormat() {
			// given
			String code = "test-code";
			String invalidToken = "this-is-not-jwt";

			KakaoTokenRes tokenRes = new KakaoTokenRes(
				"bearer", "access-token", 21599,
				null, 0, "openid",
				invalidToken);

			given(restTemplate.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(KakaoTokenRes.class)))
				.willReturn(ResponseEntity.ok(tokenRes));

			// when & then
			assertThatThrownBy(() -> kakaoApiClient.getTokenAndParseIdToken(code))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
		}
	}

	// 서명 검증 성공 케이스는 실제 JWKS 키가 필요하므로 통합 테스트에서 진행
	// 유닛 테스트에서는 실패 케이스만 검증
}
