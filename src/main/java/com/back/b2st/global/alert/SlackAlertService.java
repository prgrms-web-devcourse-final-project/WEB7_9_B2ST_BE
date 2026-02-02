package com.back.b2st.global.alert;

import static com.back.b2st.global.util.MaskingUtil.*;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.back.b2st.domain.auth.dto.response.SecurityThreatRes;
import com.back.b2st.domain.auth.dto.response.SecurityThreatRes.SeverityLevel;

import lombok.extern.slf4j.Slf4j;

/**
 * Slack Incoming Webhook 알림 서비스
 */
@Service
@Slf4j
public class SlackAlertService implements AlertService {

	// 심각도별 이모지 매핑
	private static final Map<SeverityLevel, String> SEVERITY_EMOJIS = Map.of(SeverityLevel.LOW, "🟢",
		SeverityLevel.MEDIUM, "🟡", SeverityLevel.HIGH, "🟠", SeverityLevel.CRITICAL, "🔴");

	private final RestClient restClient;
	private final boolean enabled;
	private final String webhookUrl;

	/**
	 * 생성자 - 설정 값 주입
	 */
	public SlackAlertService(@Value("${alert.enabled:false}") boolean enabled,
		@Value("${alert.slack.webhook-url:}") String webhookUrl) {
		this.enabled = enabled;
		this.webhookUrl = webhookUrl;
		this.restClient = RestClient.builder().build();
	}

	/**
	 * 보안 위협 알림 전송
	 */
	@Async
	@Override
	public void sendSecurityAlert(SecurityThreatRes threat) {
		// 설정이 안되어 있으면 무시
		if (!isConfigured())
			return;

		String emoji = SEVERITY_EMOJIS.getOrDefault(threat.severity(), "🟢");
		// 페이로드 빌드 및 전송
		String payload = buildSecurityAlertPayload(threat, emoji);
		sendToSlack(payload);
	}

	/**
	 * 계정 잠금 알림 전송
	 */
	@Async
	@Override
	public void sendAccountLockedAlert(String email, String clientIp) {
		// 설정이 안되어 있으면 무시
		if (!isConfigured())
			return;

		String payload = """
			{"text": "🔒 계정 잠금 발생\\n• 이메일: %s\\n• IP: %s"}
			""".formatted(maskEmail(email), clientIp);
		sendToSlack(payload);
	}

	// 설정 확인
	private boolean isConfigured() {
		if (!enabled || webhookUrl.isBlank()) {
			log.debug("Slack 알림 비활성화 또는 URL 미설정");
			return false;
		}
		return true;
	}

	// 보안 위협 페이로드 빌드
	private String buildSecurityAlertPayload(SecurityThreatRes threat, String emoji) {
		return """
			{
				"blocks": [
					{
						"type": "header",
						"text": {"type": "plain_text", "text": "%s 보안 위협 감지", "emoji": true}
					},
					{
						"type": "section",
						"fields": [
							{"type": "mrkdwn", "text": "*유형:*\\n%s"},
							{"type": "mrkdwn", "text": "*심각도:*\\n%s"},
							{"type": "mrkdwn", "text": "*IP:*\\n`%s`"},
							{"type": "mrkdwn", "text": "*횟수:*\\n%d"}
						]
					}
				]
			}
			""".formatted(emoji, threat.threatType(), threat.severity(), threat.clientIp(), threat.count());
	}

	// Slack으로 페이로드 전송
	private void sendToSlack(String payload) {
		try {
			restClient.post()
				.uri(webhookUrl)
				.contentType(MediaType.APPLICATION_JSON)
				.body(payload)
				.retrieve()
				.toBodilessEntity();
			log.info("Slack 알림 발송 완료");
		} catch (Exception e) {
			log.error("Slack 알림 발송 실패: {}", e.getMessage());
		}
	}
}
