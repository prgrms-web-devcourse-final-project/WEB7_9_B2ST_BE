package com.back.b2st.domain.member.listener;

import static com.back.b2st.global.util.MaskingUtil.*;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.member.dto.event.SignupEvent;
import com.back.b2st.domain.member.entity.SignupLog;
import com.back.b2st.domain.member.repository.SignupLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SignupEventListener {

	public final SignupLogRepository signupLogRepository;

	/**
	 * 회원 가입 이벤트 처리
	 * - Async 비동기하고 트랜잭션 메인이랑 분리
	 * - try catch는 예외 발생할 때 가입 프로세스에 영향 안 주고 삼키려고
	 */
	@Async("signupEventExecutor")
	@EventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handleSignupEvent(SignupEvent event) {
		try {
			SignupLog signupLog = SignupLog.builder()
				.email(event.email())
				.clientIp(event.clientIp())
				.createdAt(event.occurredAt())
				.build();

			signupLogRepository.save(signupLog);

			log.info("가입 로그 저장 완료: email={}, IP={}", maskEmail(event.email()), event.clientIp());
		} catch (Exception e) {
			log.error("가입 로그 저장 실패: {}", e.getMessage(), e);
		}
	}
}
