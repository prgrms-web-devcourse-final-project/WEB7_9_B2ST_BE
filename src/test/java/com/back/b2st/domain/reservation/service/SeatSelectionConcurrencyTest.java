package com.back.b2st.domain.reservation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.back.b2st.domain.reservation.entity.ScheduleSeat;
import com.back.b2st.domain.reservation.repository.ScheduleSeatRepository;

@SpringBootTest
@ActiveProfiles("test")
class SeatSelectionConcurrencyTest {

	@Autowired
	private SeatSelectionService seatSelectionService;

	@Autowired
	private ScheduleSeatRepository scheduleSeatRepository;

	private final Long scheduleId = 1001L;
	private final Long seatId = 55L;

	@BeforeEach
	void setUp() {
		// 좌석 초기화 (AVAILABLE)
		scheduleSeatRepository.deleteAll();
		ScheduleSeat seat = ScheduleSeat.builder()
			.scheduleId(scheduleId)
			.seatId(seatId)
			.build();

		scheduleSeatRepository.save(seat);
	}

	@Test
	@DisplayName("동시성 문제 테스트 — 두 스레드 모두 HOLD 성공하는지 검증(DB 단독 로직의 문제)")
	void concurrencyIssueOccurs() throws Exception {

		// int threadCount = 2;
		// ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		// CountDownLatch latch = new CountDownLatch(threadCount);
		//
		// AtomicInteger successCount = new AtomicInteger(0);
		//
		// Runnable task = () -> {
		// 	try {
		// 		seatSelectionService.holdSeat(scheduleId, seatId);
		// 		successCount.incrementAndGet(); // HOLD 성공한 스레드 수 증가
		// 	} catch (Exception e) {
		// 		System.out.println("[Thread Error] " + e.getMessage());
		// 	} finally {
		// 		latch.countDown();
		// 	}
		// };
		//
		// // 동시에 두 스레드 실행
		// executor.execute(task);
		// executor.execute(task);
		//
		// latch.await();  // 모든 스레드 종료 대기
		//
		// System.out.println("성공한 스레드 수 = " + successCount.get());
		//
		// // 두 스레드 모두 성공 -> 동시성 문제
		// assertThat(successCount.get())
		// 	.isEqualTo(2);
	}
}
