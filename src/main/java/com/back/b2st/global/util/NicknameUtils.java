package com.back.b2st.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NicknameUtils {

	private static final int MAX_NICKNAME_LENGTH = 20;

	public static String sanitize(String nickname, String defaultNickname) {
		if (nickname == null || nickname.isBlank()) {
			return defaultNickname;
		}

		String sanitized = nickname;

		// 제어 문자 제거
		sanitized = sanitized.replaceAll("[\\p{Cntrl}]", "");
		// 앞뒤 공백 제거
		sanitized = sanitized.trim();
		// 길이 제한
		if (sanitized.length() > MAX_NICKNAME_LENGTH) {
			sanitized = sanitized.substring(0, MAX_NICKNAME_LENGTH);
		}
		// 정제 후 빈 문자열이면 기본값
		if (sanitized.isBlank()) {
			return defaultNickname;
		}
		return sanitized;
	}

	public static boolean isValidNickname(String nickname) {
		if (nickname == null || nickname.isBlank()) {
			return false;
		}
		if (nickname.length() > MAX_NICKNAME_LENGTH) {
			return false;
		}
		if (nickname.matches(".*[\\p{Cntrl}].*")) {
			return false;
		}
		return true;
	}
}
