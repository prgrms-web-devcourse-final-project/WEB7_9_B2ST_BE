package com.back.b2st.domain.scheduleseat.service;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;

@SpringBootTest
@ActiveProfiles("test")
class ScheduleSeatDistributedLockIntegrationTest {

	@Autowired
	private ScheduleSeatStateService scheduleSeatStateService;

	@Autowired
	private ScheduleSeatRepository scheduleSeatRepository;

	@Test
	@DisplayName("동일 좌석에 동시 HOLD 요청 시 1명만 성공해야 한다")
	void distributed_lock_only_one_wins() throws Exception {
		// given (테스트용 scheduleSeat 하나 준비: status=AVAILABLE)
		Long scheduleId = 1L;
		Long seatId = 7L;

		ScheduleSeat seat = scheduleSeatRepository.findByScheduleIdAndSeatId(scheduleId, seatId)
			.orElseThrow();
		assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);

		int threads = 100;
		ExecutorService pool = Executors.newFixedThreadPool(threads);

		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);

		List<Future<Boolean>> futures = new ArrayList<>();

		for (int i = 0; i < threads; i++) {
			final long memberId = i + 1L;
			futures.add(pool.submit(() -> {
				ready.countDown();
				start.await();

				try {
					scheduleSeatStateService.holdSeat(memberId, scheduleId, seatId);
					return true; // 성공
				} catch (Exception e) {
					return false; // 실패(락 실패/이미 HOLD 등)
				} finally {
					done.countDown();
				}
			}));
		}

		ready.await();   // 모두 준비
		start.countDown(); // 동시에 시작
		done.await();

		// then
		long successCount = futures.stream().filter(f -> {
			try {
				return f.get();
			} catch (Exception e) {
				return false;
			}
		}).count();

		assertThat(successCount).isEqualTo(1);

		ScheduleSeat after = scheduleSeatRepository.findByScheduleIdAndSeatId(scheduleId, seatId).orElseThrow();
		assertThat(after.getStatus()).isEqualTo(SeatStatus.HOLD);

		pool.shutdownNow();
	}
}
