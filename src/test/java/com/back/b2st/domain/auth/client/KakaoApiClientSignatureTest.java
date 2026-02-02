package com.back.b2st.domain.auth.client;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

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
import org.springframework.web.client.RestTemplate;

import com.back.b2st.domain.auth.dto.oauth.KakaoIdTokenPayload;
import com.back.b2st.domain.auth.dto.oauth.KakaoTokenRes;
import com.back.b2st.domain.auth.error.AuthErrorCode;
import com.back.b2st.global.error.exception.BusinessException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@ExtendWith(MockitoExtension.class)
@DisplayName("KakaoApiClientImpl - ID Token 서명 검증 테스트")
class KakaoApiClientSignatureTest {

	private static final String CLIENT_ID = "test-client-id";
	private static final String ISSUER = "https://kauth.kakao.com";
	private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
	private static final String REDIRECT_URI = "http://localhost:8080/callback";
	private static final String KID = "test-key-id";
	private KakaoApiClientImpl kakaoApiClient;
	@Mock
	private RestTemplate restTemplate;
	@Mock
	private KakaoJwksClient jwksClient;
	// 테스트용 RSA 키 쌍
	private RSAPublicKey publicKey;
	private RSAPrivateKey privateKey;
	private RSAKey rsaKey;

	@BeforeEach
	void setUp() throws Exception {
		kakaoApiClient = new KakaoApiClientImpl(restTemplate, jwksClient);

		ReflectionTestUtils.setField(kakaoApiClient, "clientId", CLIENT_ID);
		ReflectionTestUtils.setField(kakaoApiClient, "clientSecret", "test-secret");
		ReflectionTestUtils.setField(kakaoApiClient, "redirectUri", REDIRECT_URI);
		ReflectionTestUtils.setField(kakaoApiClient, "tokenUri", TOKEN_URI);
		ReflectionTestUtils.setField(kakaoApiClient, "issuer", ISSUER);

		// RSA 키 쌍 생성
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048);
		KeyPair keyPair = keyGen.generateKeyPair();
		publicKey = (RSAPublicKey)keyPair.getPublic();
		privateKey = (RSAPrivateKey)keyPair.getPrivate();

		// JWK 형태로 변환
		rsaKey = new RSAKey.Builder(publicKey)
			.privateKey(privateKey)
			.keyID(KID)
			.build();
	}

	private String createValidIdToken(String sub, String email, String nickname, String nonce) throws Exception {
		return createIdTokenWithKey(sub, email, nickname, privateKey, KID, ISSUER, CLIENT_ID, nonce,
			new Date(System.currentTimeMillis() + 3600000)); // 1시간 후 만료
	}

	private String createExpiredIdToken(String sub, String email, String nickname) throws Exception {
		return createIdTokenWithKey(sub, email, nickname, privateKey, KID, ISSUER, CLIENT_ID, null,
			new Date(System.currentTimeMillis() - 3600000)); // 1시간 전 만료
	}

	// 헬퍼 메서드

	private String createIdTokenWithWrongIssuer(String sub, String email, String nickname) throws Exception {
		return createIdTokenWithKey(sub, email, nickname, privateKey, KID,
			"https://wrong-issuer.com", CLIENT_ID, null,
			new Date(System.currentTimeMillis() + 3600000));
	}

	private String createIdTokenWithWrongAudience(String sub, String email, String nickname) throws Exception {
		return createIdTokenWithKey(sub, email, nickname, privateKey, KID,
			ISSUER, "wrong-client-id", null,
			new Date(System.currentTimeMillis() + 3600000));
	}

	private String createIdTokenWithKey(String sub, String email, String nickname,
		RSAPrivateKey signingKey, String kid) throws Exception {
		return createIdTokenWithKey(sub, email, nickname, signingKey, kid, ISSUER, CLIENT_ID, null,
			new Date(System.currentTimeMillis() + 3600000));
	}

	private String createIdTokenWithKey(String sub, String email, String nickname,
		RSAPrivateKey signingKey, String kid, String issuer, String audience,
		String nonce, Date expiration) throws Exception {

		JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
			.issuer(issuer)
			.audience(audience)
			.subject(sub)
			.issueTime(new Date())
			.expirationTime(expiration)
			.claim("auth_time", System.currentTimeMillis() / 1000)
			.claim("nickname", nickname)
			.claim("email", email);

		if (nonce != null) {
			claimsBuilder.claim("nonce", nonce);
		}

		JWTClaimsSet claims = claimsBuilder.build();

		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
			.keyID(kid)
			.build();

		SignedJWT signedJWT = new SignedJWT(header, claims);
		signedJWT.sign(new RSASSASigner(signingKey));

		return signedJWT.serialize();
	}

	@Nested
	@DisplayName("ID Token 서명 검증 성공 케이스")
	class SignatureVerificationSuccessTest {

		@Test
		@DisplayName("성공 - 유효한 RSA 서명이 있는 ID Token 파싱")
		void success_validSignature() throws Exception {
			// given
			String code = "test-code";

			// 유효한 ID Token 생성
			String idToken = createValidIdToken(
				"123456789", "test@kakao.com", "테스트유저", null);

			KakaoTokenRes tokenRes = new KakaoTokenRes(
				"bearer", "access-token", 21599,
				"refresh-token", 5183999, "openid",
				idToken);

			given(restTemplate.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(KakaoTokenRes.class)))
				.willReturn(ResponseEntity.ok(tokenRes));
			given(jwksClient.getKey(KID)).willReturn(rsaKey);

			// when
			KakaoIdTokenPayload result = kakaoApiClient.getTokenAndParseIdToken(code);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getKakaoId()).isEqualTo(123456789L);
			assertThat(result.email()).isEqualTo("test@kakao.com");
			assertThat(result.nickname()).isEqualTo("테스트유저");
			verify(jwksClient).getKey(KID);
		}

		@Test
		@DisplayName("성공 - nonce가 포함된 ID Token 파싱")
		void success_withNonce() throws Exception {
			// given
			String code = "test-code";
			String nonce = "test-nonce-12345";

			String idToken = createValidIdToken(
				"987654321", "nonce@kakao.com", "논스유저", nonce);

			KakaoTokenRes tokenRes = new KakaoTokenRes(
				"bearer", "access-token", 21599,
				null, 0, "openid",
				idToken);

			given(restTemplate.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(KakaoTokenRes.class)))
				.willReturn(ResponseEntity.ok(tokenRes));
			given(jwksClient.getKey(KID)).willReturn(rsaKey);

			// when
			KakaoIdTokenPayload result = kakaoApiClient.getTokenAndParseIdToken(code);

			// then
			assertThat(result.nonce()).isEqualTo(nonce);
		}
	}

	@Nested
	@DisplayName("ID Token 서명 검증 실패 케이스")
	class SignatureVerificationFailTest {

		@Test
		@DisplayName("실패 - JWKS에서 해당 kid를 찾을 수 없음")
		void fail_keyNotFound() throws Exception {
			// given
			String code = "test-code";
			String idToken = createValidIdToken(
				"123456789", "test@kakao.com", "유저", null);

			KakaoTokenRes tokenRes = new KakaoTokenRes(
				"bearer", "access-token", 21599,
				null, 0, "openid", idToken);

			given(restTemplate.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(KakaoTokenRes.class)))
				.willReturn(ResponseEntity.ok(tokenRes));
			given(jwksClient.getKey(KID)).willReturn(null); // 키 없음

			// when & then
			assertThatThrownBy(() -> kakaoApiClient.getTokenAndParseIdToken(code))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
		}

		@Test
		@DisplayName("실패 - 서명이 다른 키로 생성됨 (위조 토큰)")
		void fail_wrongSignature() throws Exception {
			// given
			String code = "test-code";

			// 다른 키로 서명된 토큰 생성
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(2048);
			KeyPair wrongKeyPair = keyGen.generateKeyPair();
			RSAPrivateKey wrongPrivateKey = (RSAPrivateKey)wrongKeyPair.getPrivate();

			String idToken = createIdTokenWithKey(
				"123456789", "test@kakao.com", "위조유저",
				wrongPrivateKey, KID);

			KakaoTokenRes tokenRes = new KakaoTokenRes(
				"bearer", "access-token", 21599,
				null, 0, "openid", idToken);

			given(restTemplate.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(KakaoTokenRes.class)))
				.willReturn(ResponseEntity.ok(tokenRes));
			given(jwksClient.getKey(KID)).willReturn(rsaKey); // 정상 검증 키 반환

			// when & then
			assertThatThrownBy(() -> kakaoApiClient.getTokenAndParseIdToken(code))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
		}

		@Test
		@DisplayName("실패 - 만료된 토큰")
		void fail_expiredToken() throws Exception {
			// given
			String code = "test-code";
			String expiredToken = createExpiredIdToken(
				"123456789", "test@kakao.com", "만료유저");

			KakaoTokenRes tokenRes = new KakaoTokenRes(
				"bearer", "access-token", 21599,
				null, 0, "openid", expiredToken);

			given(restTemplate.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(KakaoTokenRes.class)))
				.willReturn(ResponseEntity.ok(tokenRes));
			given(jwksClient.getKey(KID)).willReturn(rsaKey);

			// when & then
			assertThatThrownBy(() -> kakaoApiClient.getTokenAndParseIdToken(code))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
		}

		@Test
		@DisplayName("실패 - issuer 불일치")
		void fail_wrongIssuer() throws Exception {
			// given
			String code = "test-code";
			String tokenWithWrongIssuer = createIdTokenWithWrongIssuer(
				"123456789", "test@kakao.com", "유저");

			KakaoTokenRes tokenRes = new KakaoTokenRes(
				"bearer", "access-token", 21599,
				null, 0, "openid", tokenWithWrongIssuer);

			given(restTemplate.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(KakaoTokenRes.class)))
				.willReturn(ResponseEntity.ok(tokenRes));
			given(jwksClient.getKey(KID)).willReturn(rsaKey);

			// when & then
			assertThatThrownBy(() -> kakaoApiClient.getTokenAndParseIdToken(code))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
		}

		@Test
		@DisplayName("실패 - audience 불일치")
		void fail_wrongAudience() throws Exception {
			// given
			String code = "test-code";
			String tokenWithWrongAud = createIdTokenWithWrongAudience(
				"123456789", "test@kakao.com", "유저");

			KakaoTokenRes tokenRes = new KakaoTokenRes(
				"bearer", "access-token", 21599,
				null, 0, "openid", tokenWithWrongAud);

			given(restTemplate.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(KakaoTokenRes.class)))
				.willReturn(ResponseEntity.ok(tokenRes));
			given(jwksClient.getKey(KID)).willReturn(rsaKey);

			// when & then
			assertThatThrownBy(() -> kakaoApiClient.getTokenAndParseIdToken(code))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
		}
	}
}
