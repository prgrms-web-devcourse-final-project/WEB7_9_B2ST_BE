package com.back.b2st.global.util;

import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MaskingUtil {

	// tester@gmail.com -> te****@gmail.com
	public static String maskEmail(String email) {
		if (!StringUtils.hasText(email))
			return "";
		// 앞 2글자 이후로 @ 전 까지 마스킹
		return email.replaceAll("(?<=.{2}).(?=.*@)", "*");
	}

	// 1234567890 -> 123****890
	public static String maskAccountNumber(String accountNumber) {
		if (!StringUtils.hasText(accountNumber))
			return "";
		String prefix = accountNumber.substring(0, 3);
		String suffix = accountNumber.substring(accountNumber.length() - 3);
		String masked = "*".repeat(accountNumber.length() - 6);
		return prefix + masked + suffix;
	}

	// 홍길동 -> 홍*동, 남궁민수 -> 남궁*수, 외자 -> 외*
	public static String maskName(String name) {
		if (!StringUtils.hasText(name) || name.length() < 2)
			return name;
		if (name.length() == 2)
			return name.charAt(0) + "*";

		String prefix = name.substring(0, 1);
		String suffix = name.substring(name.length() - 1);
		String masked = "*".repeat(name.length() - 2);
		return prefix + masked + suffix;
	}

}
