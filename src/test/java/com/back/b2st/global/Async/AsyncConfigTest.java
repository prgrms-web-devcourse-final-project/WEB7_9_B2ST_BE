package com.back.b2st.global.Async;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigTest {

	private final AsyncConfig asyncConfig = new AsyncConfig();

	@Test
	@DisplayName("emailExecutor 빈이 올바르게 구성된다")
	void emailExecutor_configuration() {
		// when
		Executor executor = asyncConfig.emailExecutor();

		// then
		assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);

		ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
		assertThat(taskExecutor.getCorePoolSize()).isEqualTo(5);
		assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(10);
		assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("email-");
	}

	@Test
	@DisplayName("emailExecutor의 ThreadPoolExecutor가 초기화된다")
	void emailExecutor_initialized() {
		// when
		Executor executor = asyncConfig.emailExecutor();

		// then
		ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
		ThreadPoolExecutor threadPoolExecutor = taskExecutor.getThreadPoolExecutor();

		assertThat(threadPoolExecutor).isNotNull();
		assertThat(threadPoolExecutor.getCorePoolSize()).isEqualTo(5);
		assertThat(threadPoolExecutor.getMaximumPoolSize()).isEqualTo(10);
	}

	@Test
	@DisplayName("emailExecutor의 대기열 용량이 50이다")
	void emailExecutor_queueCapacity() {
		// when
		Executor executor = asyncConfig.emailExecutor();

		// then
		ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
		// QueueCapacity는 직접 getter가 없으므로 ThreadPoolExecutor의 Queue로 확인
		ThreadPoolExecutor threadPoolExecutor = taskExecutor.getThreadPoolExecutor();
		assertThat(threadPoolExecutor.getQueue().remainingCapacity()).isEqualTo(50);
	}
}
