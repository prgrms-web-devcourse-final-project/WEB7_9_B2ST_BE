package com.back.b2st.domain.scheduleseat.service;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("동시성 문제 재현용 테스트 — 비활성화")
class SeatSelectionConcurrencyTest {

	@Autowired
	private ScheduleSeatStateService scheduleSeatStateService;

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

		int threadCount = 2;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		AtomicInteger successCount = new AtomicInteger(0);

		Runnable task = () -> {
			try {
				scheduleSeatStateService.changeToHold(scheduleId, seatId);
				successCount.incrementAndGet(); // HOLD 성공한 스레드 수 증가
			} catch (Exception e) {
				System.out.println("[Thread Error] " + e.getMessage());
			} finally {
				latch.countDown();
			}
		};

		// 동시에 두 스레드 실행
		executor.execute(task);
		executor.execute(task);

		latch.await();  // 모든 스레드 종료 대기

		System.out.println("성공한 스레드 수 = " + successCount.get());

		// 두 스레드 모두 성공 -> 동시성 문제
		assertThat(successCount.get())
			.isEqualTo(2);
	}
}
