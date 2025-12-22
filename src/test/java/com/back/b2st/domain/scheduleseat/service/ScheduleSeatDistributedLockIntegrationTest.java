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
@SpringBootTest
@ActiveProfiles("test")
class ScheduleSeatDistributedLockIntegrationTest {

	private static final String REDIS_PASSWORD = "tt_redis_pass";

	@Container
	static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
		.withExposedPorts(6379)
		.withCommand("redis-server", "--requirepass", REDIS_PASSWORD);

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
	@DisplayName("동일 좌석에 동시 HOLD 요청 시 1명만 성공해야 한다")
	void distributed_lock_only_one_wins() throws Exception {
		Long scheduleId = 1L;
		Long seatId = 7L;

		scheduleSeatRepository.saveAndFlush(
			ScheduleSeat.builder()
				.scheduleId(scheduleId)
				.seatId(seatId)
				.build()
		);

		int threads = 50;
		ExecutorService pool = Executors.newFixedThreadPool(threads);

		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);

		Queue<Throwable> errors = new ConcurrentLinkedQueue<>();
		List<Future<Boolean>> futures = new ArrayList<>();

		for (int i = 0; i < threads; i++) {
			final long memberId = i + 1L;
			futures.add(pool.submit(() -> {
				ready.countDown();
				start.await();
				try {
					scheduleSeatStateService.holdSeat(memberId, scheduleId, seatId);
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
		pool.shutdownNow();

		long successCount = 0;
		for (Future<Boolean> f : futures) {
			if (Boolean.TRUE.equals(f.get())) {
				successCount++;
			}
		}

		if (successCount == 0 && !errors.isEmpty()) {
			Throwable first = errors.peek();
			fail("성공이 0명입니다. 첫 예외: " + first.getClass().getName() + " / " + first.getMessage());
		}

		assertThat(successCount).isEqualTo(1);

		ScheduleSeat after = scheduleSeatRepository.findByScheduleIdAndSeatId(scheduleId, seatId).orElseThrow();
		assertThat(after.getStatus()).isEqualTo(SeatStatus.HOLD);
	}
}
