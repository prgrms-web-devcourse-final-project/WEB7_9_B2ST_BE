package com.back.b2st.global.alert;

import com.back.b2st.domain.auth.dto.response.SecurityThreatRes;

public interface AlertService {
	void sendSecurityAlert(SecurityThreatRes threat);

	void sendAccountLockedAlert(String email, String clientIp);
}
