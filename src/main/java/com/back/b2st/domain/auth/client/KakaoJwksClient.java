package com.back.b2st.domain.auth.client;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;

import lombok.extern.slf4j.Slf4j;

// 카카오 JWKS 조회 클라이언트
@Slf4j
@Component
public class KakaoJwksClient {

	@Value("${oauth.kakao.jwks-uri:https://kauth.kakao.com/.well-known/jwks.json}")
	private String jwksUri;

	// 님버스 라이브러리 제공 JWK 소스
	// JWK = JSON Web Key (JSON 형태로 표현된 암호화 키)
	// 카카오 서버랑 통신하고 캐시 관리
	private volatile JWKSource<SecurityContext> jwksSource;

	// 지연 초기화 (Lazy Initialization)
	private JWKSource<SecurityContext> getJwksSource() {
		if (jwksSource == null) {
			synchronized (this) {
				if (jwksSource == null) {
					initJwksSource();
				}
			}
		}
		return jwksSource;
	}

	private void initJwksSource() {
		try {
			jwksSource = JWKSourceBuilder
					.create(new URL(jwksUri))
					// TTL 24시간, 리프레시 타임아웃 1시간
					// cache(lifespan, refreshTimeout) - lifespan이 더 길어야 함
					.cache(TimeUnit.HOURS.toMillis(24), TimeUnit.HOURS.toMillis(1))
					// 속도 제한 비활성화
					.rateLimited(false)
					.build();

			log.info("[Kakao JWKS] 초기화 완료: {}", jwksUri);
		} catch (Exception e) {
			log.error("[Kakao JWKS] 초기화 실패", e);
			throw new IllegalStateException("JWKS 클라이언트 초기화 실패", e);
		}
	}

	// kid로 공개키 조회
	public JWK getKey(String kid) {
		// null 또는 빈 kid는 조회 불가
		if (kid == null || kid.isBlank()) {
			log.warn("[Kakao JWKS] kid가 null 또는 빈 문자열");
			return null;
		}

		try {
			// 검색조건. id가 'kid'인 키
			JWKMatcher matcher = new JWKMatcher.Builder()
					.keyID(kid)
					.build();

			// 검색 실행
			var keys = getJwksSource().get(new JWKSelector(matcher), null);

			if (keys.isEmpty()) {
				log.warn("[Kakao JWKS] kid={} 에 해당하는 키 없음", kid);
				return null;
			}

			// 1개지만 리스트 반환이라
			return keys.get(0);
		} catch (Exception e) {
			log.error("[Kakao JWKS] 키 조회 실패: kid={}", kid, e);
			return null;
		}
	}
}
