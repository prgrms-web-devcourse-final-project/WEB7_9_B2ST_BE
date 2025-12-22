package com.back.b2st.domain.auth.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.back.b2st.domain.auth.dto.oauth.KakaoIdTokenPayload;
import com.back.b2st.domain.auth.dto.oauth.KakaoTokenRes;
import com.back.b2st.domain.auth.error.AuthErrorCode;
import com.back.b2st.global.error.exception.BusinessException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoApiClientImpl implements KakaoApiClient {

	// http 통신용 RestTemplate
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final KakaoJwksClient jwksClient;

	@Value("${oauth.kakao.client-id}")
	private String clientId;
	@Value("${oauth.kakao.client-secret}")
	private String clientSecret;
	@Value("${oauth.kakao.redirect-uri}")
	private String redirectUri;
	@Value("${oauth.kakao.token-uri}")
	private String tokenUri;
	@Value("${oauth.kakao.user-info-uri}")
	private String userInfoUri;
	@Value("${oauth.kakao.issuer}")
	private String issuer;

	@Override
	public KakaoIdTokenPayload getTokenAndParseIdToken(String code) {
		// 토큰 발급
		KakaoTokenRes tokenRes = getTokenWithOpenId(code);

		if (tokenRes.idToken() == null || tokenRes.idToken().isBlank()) {
			log.warn("[Kakao] id_token 없음 - scope에 openid가 포함되었는지 확인 필요");
			throw new BusinessException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
		}

		// id token 파싱
		return parseIdToken(tokenRes.idToken());
	}

	private KakaoTokenRes getTokenWithOpenId(String code) {
		// http 헤더 설정
		// 카카오 토큰 api는 form-urlencoded 형식만 허용
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		// 리퀘 파라미터
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", "authorization_code"); // OAuth 타입 고정
		params.add("client_id", clientId); // 앱 식별자
		params.add("client_secret", clientSecret); // 앱 시크릿키
		params.add("redirect_uri", redirectUri); // 콜백
		params.add("code", code); // 인가 코드

		// http 리퀘 객체 생성
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

		try {
			// 카카오 서버로 post 요청
			ResponseEntity<KakaoTokenRes> response = restTemplate.postForEntity(tokenUri, request, KakaoTokenRes.class);

			// res 검증
			if (response.getBody() == null) {
				throw new BusinessException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
			}

			log.info("[Kakao] 토큰 발급 성공");
			return response.getBody();

		} catch (RestClientException e) {
			log.error("[Kakao] 토큰 발급 실패: {}", e.getMessage());
			throw new BusinessException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
		}
	}

	// id_token 파싱 및 서명 검증 메서드
	// 순서: jwt 형식(header.payload.signature) 검증 -> 서명 검증(카카오 공개키 사용)
	// -> iss 검증 -> aud 검증 -> exp 검증
	private KakaoIdTokenPayload parseIdToken(String idToken) {
		try {
			// signature 포함 jwt 다루는 클래스
			SignedJWT signedJWT = SignedJWT.parse(idToken);

			// kid 추출
			String kid = signedJWT.getHeader().getKeyID();
			if (kid == null) {
				log.warn("[Kakao] ID Token에 kid 없음");
				throw new BusinessException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
			}

			// 원본 요청
			JWK jwk = jwksClient.getKey(kid);
			if (jwk == null) {
				// 위조 토큰
				throw new BusinessException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
			}

			// 서명 검증
			// RSA 검증기에 카카오 공개키 물리기
			JWSVerifier verifier = new RSASSAVerifier(jwk.toRSAKey());

			// 검증 수행
			// payload를 암호화했을 때, signature랑 일치하는지. 카카오 공개키로 풀리냐 안풀리냐 체크
			boolean verified = signedJWT.verify(verifier);

			if (!verified) {
				// 불일치. 내용 조작이나 서명 위조
				log.warn("[Kakao] ID Token 서명 검증 실패");
				throw new BusinessException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
			}

			log.info("[Kakao] ID Token 서명 검증 성공");

			// 내용물 체크
			JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

			// 발급자 검증
			if (!issuer.equals(claims.getIssuer())) {
				log.warn("[Kakao] ID Token 발급자 불일치: expected={}, actual={}", issuer, claims.getIssuer());
				throw new BusinessException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
			}

			// 대상 검증
			if (!claims.getAudience().contains(clientId)) {
				log.warn("[Kakao] ID Token 대상 불일치: expected={}, actual={}", clientId, claims.getAudience());
				throw new BusinessException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
			}

			// 만료 검증
			if (claims.getExpirationTime() != null && claims.getExpirationTime().before(new java.util.Date())) {
				log.warn("[Kakao] ID Token 만료됨");
				throw new BusinessException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
			}

			// dto 생성
			return new KakaoIdTokenPayload(
				claims.getIssuer(),
				claims.getAudience().get(0),
				claims.getSubject(), // 카카오 회원번호
				claims.getIssueTime() != null ? claims.getIssueTime().getTime() / 1000 : null,
				claims.getExpirationTime() != null ? claims.getExpirationTime().getTime() / 1000 : null,
				claims.getDateClaim("auth_time") != null ?
					claims.getDateClaim("auth_time").getTime() / 1000 : null,
				claims.getStringClaim("nonce"),
				claims.getStringClaim("nickname"),
				claims.getStringClaim("picture"),
				claims.getStringClaim("email")
			);

		} catch (Exception e) {
			log.error("[Kakao] ID Token 파싱 실패: {}", e.getMessage());
			throw new BusinessException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
		}
	}

	// //
	//
	// // 토큰 발급 api 호출
	// // POST https://kauth.kakao.com/oauth/token
	// // Content-Type: application/x-www-form-urlencoded
	// @Override
	// public KakaoTokenRes getToken(String code) {
	// 	getTokenWithOpenId(code);
	// }
	//
	// // 카카오 userInfo api 호출
	// // GET https://kapi.kakao.com/v2/user/me
	// // Authorization: Bearer {accessToken}
	// @Override
	// public KakaoUserInfo getUserInfo(String accessToken) {
	// 	// 헤더 설정
	// 	HttpHeaders headers = new HttpHeaders();
	// 	headers.setBearerAuth(accessToken);
	// 	headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
	//
	// 	HttpEntity<Void> request = new HttpEntity<>(headers);
	//
	// 	try {
	// 		ResponseEntity<KakaoUserInfo> response = restTemplate.exchange(userInfoUri, HttpMethod.GET, request,
	// 			KakaoUserInfo.class);
	//
	// 		if (response.getBody() == null) {
	// 			throw new BusinessException(AuthErrorCode.OAUTH_USER_INFO_FAILED);
	// 		}
	// 		log.info("[Kakao] 사용자 정보 조회 성공: kakaoId={}", response.getBody().id());
	// 		return response.getBody();
	// 	} catch (RestClientException e) {
	// 		log.error("[Kakao] 사용자 정보 조회 실패: {}", e.getMessage());
	// 		throw new BusinessException(AuthErrorCode.OAUTH_USER_INFO_FAILED);
	// 	}
	// }
}
