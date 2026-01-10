package com.back.b2st.global.util;

import com.back.b2st.global.error.code.CommonErrorCode;
import com.back.b2st.global.error.exception.BusinessException;
import com.back.b2st.security.UserPrincipal;

public final class SecurityUtils {

	private SecurityUtils() {}

	public static Long requireUserId(UserPrincipal principal) {
		if (principal == null) {
			throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
		}
		return principal.getId();
	}
}
