package com.back.b2st.global.alert;

import com.back.b2st.domain.auth.dto.response.SecurityThreatRes;

public sealed interface AlertService permits SlackAlertService {
	void sendSecurityAlert(SecurityThreatRes threat);

	void sendAccountLockedAlert(String email, String clientIp);
}
