package com.back.b2st.global.async;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// 비동기
@Configuration
@EnableAsync
public class AsyncConfig {

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
}
