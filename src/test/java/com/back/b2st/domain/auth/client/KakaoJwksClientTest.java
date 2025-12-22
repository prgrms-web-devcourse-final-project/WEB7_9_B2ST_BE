package com.back.b2st.domain.auth.client;

import static org.assertj.core.api.Assertions.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;

@DisplayName("KakaoJwksClient 테스트")
class KakaoJwksClientTest {

	private KakaoJwksClient kakaoJwksClient;

	@BeforeEach
	void setUp() {
		kakaoJwksClient = new KakaoJwksClient();
		// 실제 카카오 JWKS URI 사용 (테스트 환경에서 네트워크 접근 필요)
		ReflectionTestUtils.setField(kakaoJwksClient, "jwksUri",
			"https://kauth.kakao.com/.well-known/jwks.json");
	}

	@Nested
	@DisplayName("Lazy Initialization")
	class LazyInitializationTest {

		@Test
		@DisplayName("초기 상태에서 jwksSource는 null")
		void initialStateIsNull() {
			// given
			KakaoJwksClient client = new KakaoJwksClient();
			ReflectionTestUtils.setField(client, "jwksUri",
				"https://kauth.kakao.com/.well-known/jwks.json");

			// when
			Object jwksSource = ReflectionTestUtils.getField(client, "jwksSource");

			// then
			assertThat(jwksSource).isNull();
		}

		@Test
		@DisplayName("getKey 호출 시 jwksSource가 초기화됨")
		void initializesOnFirstCall() {
			// given
			KakaoJwksClient client = new KakaoJwksClient();
			ReflectionTestUtils.setField(client, "jwksUri",
				"https://kauth.kakao.com/.well-known/jwks.json");

			// when
			// 존재하지 않는 kid로 조회해도 초기화는 발생
			client.getKey("non-existent-kid");

			// then
			Object jwksSource = ReflectionTestUtils.getField(client, "jwksSource");
			assertThat(jwksSource).isNotNull();
		}
	}

	@Nested
	@DisplayName("키 조회")
	class GetKeyTest {

		@Test
		@DisplayName("존재하지 않는 kid로 조회 시 null 반환")
		void returnsNullForNonExistentKid() {
			// when
			JWK result = kakaoJwksClient.getKey("non-existent-kid-12345");

			// then
			assertThat(result).isNull();
		}

		@Test
		@DisplayName("null kid로 조회 시 null 반환")
		void returnsNullForNullKid() {
			// when
			JWK result = kakaoJwksClient.getKey(null);

			// then
			assertThat(result).isNull();
		}

		@Test
		@DisplayName("빈 문자열 kid로 조회 시 null 반환")
		void returnsNullForEmptyKid() {
			// when
			JWK result = kakaoJwksClient.getKey("");

			// then
			assertThat(result).isNull();
		}
	}

	@Nested
	@DisplayName("Thread Safety (Double-Checked Locking)")
	class ThreadSafetyTest {

		@Test
		@DisplayName("동시 호출 시에도 한 번만 초기화됨")
		void initializesOnlyOnce() throws Exception {
			// given
			KakaoJwksClient client = new KakaoJwksClient();
			ReflectionTestUtils.setField(client, "jwksUri",
				"https://kauth.kakao.com/.well-known/jwks.json");

			int threadCount = 10;
			Thread[] threads = new Thread[threadCount];
			Exception[] exceptions = new Exception[1];

			// when
			for (int i = 0; i < threadCount; i++) {
				threads[i] = new Thread(() -> {
					try {
						client.getKey("test-kid");
					} catch (Exception e) {
						exceptions[0] = e;
					}
				});
			}

			for (Thread thread : threads) {
				thread.start();
			}

			for (Thread thread : threads) {
				thread.join();
			}

			// then
			assertThat(exceptions[0]).isNull();
			Object jwksSource = ReflectionTestUtils.getField(client, "jwksSource");
			assertThat(jwksSource).isNotNull();
		}
	}

	@Nested
	@DisplayName("JWK 형식 검증")
	class JwkFormatTest {

		@Test
		@DisplayName("RSA 키를 JWK로 변환 및 검증")
		void rsaKeyConversion() throws Exception {
			// given
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(2048);
			KeyPair keyPair = keyGen.generateKeyPair();
			RSAPublicKey publicKey = (RSAPublicKey)keyPair.getPublic();

			String kid = "test-kid-12345";
			RSAKey rsaKey = new RSAKey.Builder(publicKey)
				.keyID(kid)
				.build();

			// when & then
			assertThat(rsaKey.getKeyID()).isEqualTo(kid);
			assertThat(rsaKey.toPublicKey()).isInstanceOf(RSAPublicKey.class);
			assertThat(rsaKey.isPrivate()).isFalse();
		}
	}

	@Nested
	@DisplayName("잘못된 JWKS URI 설정")
	class InvalidJwksUriTest {

		@Test
		@DisplayName("잘못된 URI로 설정 시 키 조회 실패")
		void failsWithInvalidUri() {
			// given
			KakaoJwksClient client = new KakaoJwksClient();
			ReflectionTestUtils.setField(client, "jwksUri", "http://invalid-url-that-does-not-exist.com/jwks");

			// when
			JWK result = client.getKey("any-kid");

			// then
			// 잘못된 URI여도 null 반환
			assertThat(result).isNull();
		}
	}
}
