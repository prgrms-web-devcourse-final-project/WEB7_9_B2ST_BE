package com.back.b2st.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alert")
public record AlertConfig(
	boolean enabled,
	SlackConfig slack
) {
	public record SlackConfig(String webhookUrl) {
	}
}
