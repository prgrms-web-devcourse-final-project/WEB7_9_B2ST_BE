package com.back.b2st.domain.auth.listener;

import static com.back.b2st.global.util.MaskingUtil.*;

import java.time.LocalDateTime;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.auth.dto.response.LoginEvent;
import com.back.b2st.domain.auth.entity.LoginLog;
import com.back.b2st.domain.auth.metrics.SecurityMetrics;
import com.back.b2st.domain.auth.repository.LoginLogRepository;
import com.back.b2st.domain.auth.service.SecurityThreatDetectionService;
import com.back.b2st.global.alert.AlertService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 로그인 이벤트 리스너
 * - LoginEvent 발생 시 비동기로 LoginLog DB 저장
 * - 메인 로그인 흐름과 분리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoginEventListener {

	private final LoginLogRepository loginLogRepository;
	private final SecurityThreatDetectionService threatDetectionService;
	private final AlertService alertService;
	private final SecurityMetrics securityMetrics;

	/**
	 * 로그인 이벤트 처리
	 *
	 * @Transactional(REQUIRES_NEW): 기존 트랜잭션과 별도로 새 트랜잭션 생성
	 * - 로그 저장 실패가 메인 로그인 흐름에 영향 주지 않도록
	 */
	@Async("loginEventExecutor")
	@EventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handleLoginEvent(LoginEvent event) {
		try {
			LoginLog loginLog = LoginLog.builder()
				.email(event.email())
				.clientIp(event.clientIp())
				.success(event.isSuccess())
				.failReason(determineFailReason(event))
				.attemptedAt(LocalDateTime.now())
				.build();

			loginLogRepository.save(loginLog);

			// 실패한 로그인에 대해 보안 위협 탐지
			if (!event.isSuccess()) {
				threatDetectionService.detectThreatForIp(event.clientIp())
					.ifPresent(threat -> {
						alertService.sendSecurityAlert(threat);
						securityMetrics.recordSecurityThreat(threat);
					});
			}

			log.info("로그인 로그 저장 완료: email={}, success={}, ip={}",
				maskEmail(event.email()), event.isSuccess(), event.clientIp());
		} catch (Exception e) {
			// 로그 저장 실패해도 로그인에 영향 없음
			log.error("로그인 로그 저장 실패: {}", e.getMessage(), e);
		}
	}

	/**
	 * 실패 사유 결정
	 */
	private LoginLog.FailReason determineFailReason(LoginEvent event) {
		return event.isSuccess() ? null : LoginLog.FailReason.fromErrorCode(event.errorCode());
	}
}
