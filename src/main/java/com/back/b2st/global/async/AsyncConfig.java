package com.back.b2st.global.async;

import java.util.concurrent.Executor;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

// 비동기
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

	/**
	 * 이메일 발송용 Executor 빈 등록
	 */
	@Bean
	public Executor emailExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

		// 동시 처리할 이메일 작업
		executor.setCorePoolSize(5);
		// 대기열이 가득 차면 여기까지 확장
		executor.setMaxPoolSize(10);
		// 모든 스레드 작업 여유 없을 시 대기시킬 작업 수
		executor.setQueueCapacity(50);
		// 로그에서 식별용
		executor.setThreadNamePrefix("email-");

		executor.initialize();

		return executor;
	}

	/**
	 * 로그인 이벤트 처리용 Executor 빈 등록
	 * - 로그인 로그 저장 등 비동기 처리
	 * - 메인 로그인 흐름에 영향 없도록
	 */
	@Bean
	public Executor loginEventExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

		executor.setCorePoolSize(3); // 기본 스레드 수
		executor.setMaxPoolSize(5); // 최대 스레드 수
		executor.setQueueCapacity(100); // 대기 큐
		executor.setThreadNamePrefix("login-event-");
		executor.setRejectedExecutionHandler((r, e) -> {
			// 큐가 가득 차면 버리고 로그만 남김 (로그인 성능 영향 없도록)
			LoggerFactory.getLogger("LoginEventExecutor").warn("로그인 이벤트 처리 큐 가득 참. 이벤트 무시됨.");
		});

		executor.initialize();

		return executor;
	}
}
