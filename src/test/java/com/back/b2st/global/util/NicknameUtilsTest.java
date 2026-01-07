package com.back.b2st.global.util;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("NicknameUtils 테스트")
class NicknameUtilsTest {

	private static final String DEFAULT_NICKNAME = "카카오사용자";

	@Nested
	@DisplayName("sanitize")
	class SanitizeTest {

		@Test
		@DisplayName("정상 닉네임은 그대로 반환")
		void returnAsIs_whenNormal() {
			assertThat(NicknameUtils.sanitize("홍길동", DEFAULT_NICKNAME))
				.isEqualTo("홍길동");
		}

		@Test
		@DisplayName("이모지 포함 닉네임 허용")
		void allowEmoji() {
			assertThat(NicknameUtils.sanitize("홍길동🎉", DEFAULT_NICKNAME))
				.isEqualTo("홍길동🎉");
		}

		@Test
		@DisplayName("특수문자 포함 닉네임 허용")
		void allowSpecialChars() {
			assertThat(NicknameUtils.sanitize("user_123", DEFAULT_NICKNAME))
				.isEqualTo("user_123");
		}

		@Test
		@DisplayName("null이면 기본값 반환")
		void returnDefault_whenNull() {
			assertThat(NicknameUtils.sanitize(null, DEFAULT_NICKNAME))
				.isEqualTo(DEFAULT_NICKNAME);
		}

		@Test
		@DisplayName("빈 문자열이면 기본값 반환")
		void returnDefault_whenBlank() {
			assertThat(NicknameUtils.sanitize("   ", DEFAULT_NICKNAME))
				.isEqualTo(DEFAULT_NICKNAME);
		}

		@Test
		@DisplayName("20자 초과 시 잘림")
		void truncate_whenTooLong() {
			String longName = "가나다라마바사아자차카타파하가나다라마바사아자차"; // 24자
			String result = NicknameUtils.sanitize(longName, DEFAULT_NICKNAME);
			assertThat(result).hasSize(20);
			assertThat(result).isEqualTo("가나다라마바사아자차카타파하가나다라마바");
		}

		@Test
		@DisplayName("제어 문자 제거")
		void removeControlChars() {
			assertThat(NicknameUtils.sanitize("홍길\t동\n", DEFAULT_NICKNAME))
				.isEqualTo("홍길동");
		}

		@Test
		@DisplayName("앞뒤 공백 제거")
		void trimWhitespace() {
			assertThat(NicknameUtils.sanitize("  홍길동  ", DEFAULT_NICKNAME))
				.isEqualTo("홍길동");
		}

		@Test
		@DisplayName("제어 문자만 있으면 기본값 반환")
		void returnDefault_whenOnlyControlChars() {
			assertThat(NicknameUtils.sanitize("\t\n\r", DEFAULT_NICKNAME))
				.isEqualTo(DEFAULT_NICKNAME);
		}

		@Test
		@DisplayName("일본어 닉네임 허용")
		void allowJapanese() {
			assertThat(NicknameUtils.sanitize("ユーザー", DEFAULT_NICKNAME))
				.isEqualTo("ユーザー");
		}
	}

	@Nested
	@DisplayName("isValidNickname")
	class IsValidNicknameTest {

		@Test
		@DisplayName("정상 닉네임은 true")
		void returnTrue_whenValid() {
			assertThat(NicknameUtils.isValidNickname("홍길동")).isTrue();
			assertThat(NicknameUtils.isValidNickname("user🎉")).isTrue();
		}

		@Test
		@DisplayName("null이면 false")
		void returnFalse_whenNull() {
			assertThat(NicknameUtils.isValidNickname(null)).isFalse();
		}

		@Test
		@DisplayName("빈 문자열이면 false")
		void returnFalse_whenBlank() {
			assertThat(NicknameUtils.isValidNickname("   ")).isFalse();
		}

		@Test
		@DisplayName("20자 초과면 false")
		void returnFalse_whenTooLong() {
			String longName = "가나다라마바사아자차카타파하가나다라마바사"; // 21자
			assertThat(NicknameUtils.isValidNickname(longName)).isFalse();
		}

		@Test
		@DisplayName("제어 문자 포함하면 false")
		void returnFalse_whenContainsControlChar() {
			assertThat(NicknameUtils.isValidNickname("홍길\t동")).isFalse();
		}

		@Test
		@DisplayName("정확히 20자는 true")
		void returnTrue_whenExactly20Chars() {
			String exactName = "가나다라마바사아자차카타파하가나다라마바"; // 20자
			assertThat(NicknameUtils.isValidNickname(exactName)).isTrue();
		}
	}
}
