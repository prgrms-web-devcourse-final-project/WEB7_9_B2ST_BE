package com.back.b2st.global.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class AbstractContainerBaseTest {

	static final String REDIS_IMAGE = "redis:7.0.8-alpine";

	static final GenericContainer<?> REDIS_CONTAINER;

	static {
		REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
			.withExposedPorts(6379)
			.withReuse(true); // 컨테이너 재사용 (테스트 속도 향상)
		REDIS_CONTAINER.start();
	}

	// 스프링 부트가 실행될 때, yml의 설정을 컨테이너 정보로 덮어씌움
	@DynamicPropertySource
	public static void overrideProps(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
		registry.add("spring.data.redis.port", () -> String.valueOf(REDIS_CONTAINER.getFirstMappedPort()));
	}
}
