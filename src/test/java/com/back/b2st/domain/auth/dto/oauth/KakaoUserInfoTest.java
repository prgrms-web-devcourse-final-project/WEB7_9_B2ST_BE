package com.back.b2st.domain.auth.dto.oauth;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class KakaoUserInfoTest {

	@Nested
	@DisplayName("getEmail 테스트")
	class GetEmailTest {
		@Test
		@DisplayName("정상적인 이메일 반환")
		void returnsEmail_whenAccountExists() {
			// given
			var profile = new KakaoUserInfo.KakaoProfile("닉네임", null);
			var account = new KakaoUserInfo.KakaoAccount("test@kakao.com", true, true, profile);
			var userInfo = new KakaoUserInfo(12345L, account);
			// when & then
			assertThat(userInfo.getEmail()).isEqualTo("test@kakao.com");
		}

		@Test
		@DisplayName("kakaoAccount가 null이면 null 반환")
		void returnsNull_whenAccountIsNull() {
			// given
			var userInfo = new KakaoUserInfo(12345L, null);
			// when & then
			assertThat(userInfo.getEmail()).isNull();
		}
	}

	@Nested
	@DisplayName("getNickname 테스트")
	class GetNicknameTest {
		@Test
		@DisplayName("정상적인 닉네임 반환")
		void returnsNickname_whenProfileExists() {
			// given
			var profile = new KakaoUserInfo.KakaoProfile("테스트유저", "http://image.url");
			var account = new KakaoUserInfo.KakaoAccount("test@kakao.com", true, true, profile);
			var userInfo = new KakaoUserInfo(12345L, account);
			// when & then
			assertThat(userInfo.getNickname()).isEqualTo("테스트유저");
		}

		@Test
		@DisplayName("kakaoAccount가 null이면 null 반환")
		void returnsNull_whenAccountIsNull() {
			// given
			var userInfo = new KakaoUserInfo(12345L, null);
			// when & then
			assertThat(userInfo.getNickname()).isNull();
		}

		@Test
		@DisplayName("profile이 null이면 null 반환")
		void returnsNull_whenProfileIsNull() {
			// given
			var account = new KakaoUserInfo.KakaoAccount("test@kakao.com", true, true, null);
			var userInfo = new KakaoUserInfo(12345L, account);
			// when & then
			assertThat(userInfo.getNickname()).isNull();
		}
	}

	@Nested
	@DisplayName("isEmailVerified 테스트")
	class IsEmailVerifiedTest {
		@Test
		@DisplayName("이메일 인증됨")
		void returnsTrue_whenVerified() {
			// given
			var profile = new KakaoUserInfo.KakaoProfile("닉네임", null);
			var account = new KakaoUserInfo.KakaoAccount("test@kakao.com", true, true, profile);
			var userInfo = new KakaoUserInfo(12345L, account);
			// when & then
			assertThat(userInfo.isEmailVerified()).isTrue();
		}

		@Test
		@DisplayName("이메일 미인증")
		void returnsFalse_whenNotVerified() {
			// given
			var profile = new KakaoUserInfo.KakaoProfile("닉네임", null);
			var account = new KakaoUserInfo.KakaoAccount("test@kakao.com", true, false, profile);
			var userInfo = new KakaoUserInfo(12345L, account);
			// when & then
			assertThat(userInfo.isEmailVerified()).isFalse();
		}

		@Test
		@DisplayName("isEmailVerified가 null이면 false 반환")
		void returnsFalse_whenVerifiedIsNull() {
			// given
			var profile = new KakaoUserInfo.KakaoProfile("닉네임", null);
			var account = new KakaoUserInfo.KakaoAccount("test@kakao.com", true, null, profile);
			var userInfo = new KakaoUserInfo(12345L, account);
			// when & then
			assertThat(userInfo.isEmailVerified()).isFalse();
		}

		@Test
		@DisplayName("kakaoAccount가 null이면 false 반환")
		void returnsFalse_whenAccountIsNull() {
			// given
			var userInfo = new KakaoUserInfo(12345L, null);
			// when & then
			assertThat(userInfo.isEmailVerified()).isFalse();
		}
	}

	@Nested
	@DisplayName("Record 기본 기능 테스트")
	class RecordBasicsTest {
		@Test
		@DisplayName("id 접근자 정상 작동")
		void id_accessor_works() {
			// given
			var userInfo = new KakaoUserInfo(99999L, null);
			// when & then
			assertThat(userInfo.id()).isEqualTo(99999L);
		}

		@Test
		@DisplayName("kakaoAccount 접근자 정상 작동")
		void kakaoAccount_accessor_works() {
			// given
			var profile = new KakaoUserInfo.KakaoProfile("닉네임", "http://image.url");
			var account = new KakaoUserInfo.KakaoAccount("email@test.com", true, true, profile);
			var userInfo = new KakaoUserInfo(12345L, account);
			// when & then
			assertThat(userInfo.kakaoAccount()).isEqualTo(account);
			assertThat(userInfo.kakaoAccount().email()).isEqualTo("email@test.com");
			assertThat(userInfo.kakaoAccount().isEmailValid()).isTrue();
			assertThat(userInfo.kakaoAccount().isEmailVerified()).isTrue();
			assertThat(userInfo.kakaoAccount().profile()).isEqualTo(profile);
		}

		@Test
		@DisplayName("KakaoProfile 접근자 정상 작동")
		void kakaoProfile_accessors_work() {
			// given
			var profile = new KakaoUserInfo.KakaoProfile("테스트닉네임", "http://profile.image.url");
			// when & then
			assertThat(profile.nickname()).isEqualTo("테스트닉네임");
			assertThat(profile.profileImageUrl()).isEqualTo("http://profile.image.url");
		}
	}
}
