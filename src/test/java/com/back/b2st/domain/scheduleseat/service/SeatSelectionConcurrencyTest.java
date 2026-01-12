package com.back.b2st.domain.scheduleseat.service;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;

@Testcontainers
@SpringBootTest(properties = {
	"spring.task.scheduling.enabled=false"
})
@ActiveProfiles("test")
@Disabled
class SeatSelectionConcurrencyTest {

	private static final String REDIS_PASSWORD = "testpass";

	@Container
	static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
		.withExposedPorts(6379)
		.withCommand("redis-server", "--requirepass", REDIS_PASSWORD); // 테스트용 Redis 실행

	@DynamicPropertySource
	static void redisProps(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
		registry.add("spring.data.redis.password", () -> REDIS_PASSWORD);
	}

	@Autowired
	private ScheduleSeatStateService scheduleSeatStateService;

	@Autowired
	private ScheduleSeatRepository scheduleSeatRepository;

	@Test
	@DisplayName("동일 좌석에 동시 HOLD 요청 시 1명만 성공한다")
	void concurrent_hold_only_one_success() throws Exception {

		// given: 하나의 좌석이 AVAILABLE 상태
		Long scheduleId = 1L;
		Long seatId = 1L;

		scheduleSeatRepository.deleteAll();
		scheduleSeatRepository.saveAndFlush(
			ScheduleSeat.builder()
				.scheduleId(scheduleId)
				.seatId(seatId)
				.build()
		);

		int threadCount = 50;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);

		CountDownLatch ready = new CountDownLatch(threadCount); // 모든 스레드 준비 대기
		CountDownLatch start = new CountDownLatch(1);           // 동시에 출발
		CountDownLatch done = new CountDownLatch(threadCount);  // 종료 대기

		List<Future<Boolean>> futures = new ArrayList<>();
		Queue<Throwable> errors = new ConcurrentLinkedQueue<>();

		// when: 50명이 동시에 동일 좌석 HOLD 시도
		for (int i = 0; i < threadCount; i++) {
			final long memberId = i + 1;
			futures.add(executor.submit(() -> {
				ready.countDown();
				start.await();
				try {
					scheduleSeatStateService.holdSeat(memberId, scheduleId, seatId); // 실제 서비스 진입점
					return true;
				} catch (Throwable t) {
					errors.add(t);
					return false;
				} finally {
					done.countDown();
				}
			}));
		}

		ready.await();
		start.countDown();
		done.await();
		executor.shutdownNow();

		long successCount = futures.stream()
			.filter(f -> {
				try {
					return Boolean.TRUE.equals(f.get());
				} catch (Exception e) {
					return false;
				}
			})
			.count();

		// then: 단 1명만 HOLD 성공
		assertThat(successCount).isEqualTo(1);

		ScheduleSeat after = scheduleSeatRepository
			.findByScheduleIdAndSeatId(scheduleId, seatId)
			.orElseThrow();

		assertThat(after.getStatus()).isEqualTo(SeatStatus.HOLD);
	}
}
