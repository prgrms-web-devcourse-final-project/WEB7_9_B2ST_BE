package com.back.b2st.domain.reservation.metrics;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class ReservationMetrics {
	private final MeterRegistry registry;
	private final AtomicInteger pendingCount = new AtomicInteger(0);
	// Counters
	private final Counter reservationCreatedCounter;
	private final Counter reservationCompletedCounter;
	private final Counter reservationFailedCounter;
	private final Counter reservationExpiredCounter;
	private final Counter reservationCancelledCounter;
	// Timer
	private final Timer reservationCreationTimer;

	public ReservationMetrics(MeterRegistry registry) {
		this.registry = registry;

		// 예약 생성
		this.reservationCreatedCounter = Counter.builder("reservation_total")
				.tag("action", "created")
				.description("예약 생성 횟수")
				.register(registry);

		// 예약 완료
		this.reservationCompletedCounter = Counter.builder("reservation_total")
				.tag("action", "completed")
				.description("예약 완료 횟수")
				.register(registry);

		// 예약 실패
		this.reservationFailedCounter = Counter.builder("reservation_total")
				.tag("action", "failed")
				.description("예약 실패 횟수")
				.register(registry);

		// 예약 만료
		this.reservationExpiredCounter = Counter.builder("reservation_total")
				.tag("action", "expired")
				.description("예약 만료 횟수")
				.register(registry);

		// 예약 취소
		this.reservationCancelledCounter = Counter.builder("reservation_total")
				.tag("action", "cancelled")
				.description("예약 취소 횟수")
				.register(registry);

		// 현재 PENDING 예약 수
		Gauge.builder("reservation_pending_count", pendingCount, AtomicInteger::get)
				.description("현재 PENDING 상태 예약 수")
				.register(registry);

		// 예약 생성 처리 시간
		this.reservationCreationTimer = Timer.builder("reservation_creation_duration")
				.description("예약 생성 처리 시간")
				.register(registry);
	}

	/** 예약 생성 기록 */
	public void recordCreated() {
		reservationCreatedCounter.increment();
		pendingCount.incrementAndGet();
	}

	/** 예약 완료 기록 */
	public void recordCompleted() {
		reservationCompletedCounter.increment();
		pendingCount.decrementAndGet();
	}

	/** 예약 실패 기록 */
	public void recordFailed() {
		reservationFailedCounter.increment();
		pendingCount.decrementAndGet();
	}

	/** 예약 만료 기록 (배치용) */
	public void recordExpired(int count) {
		for (int i = 0; i < count; i++) {
			reservationExpiredCounter.increment();
		}
		pendingCount.addAndGet(-count);
	}

	/** 예약 취소 기록 */
	public void recordCancelled() {
		reservationCancelledCounter.increment();
		pendingCount.decrementAndGet();
	}

	/** 예약 생성 처리 시간 측정 시작 */
	public Timer.Sample startCreationTimer() {
		return Timer.start(registry);
	}

	/** 예약 생성 처리 시간 측정 종료 */
	public void stopCreationTimer(Timer.Sample sample) {
		sample.stop(reservationCreationTimer);
	}

	/** PENDING 예약 수 설정 (배치용) */
	public void setPendingCount(int count) {
		pendingCount.set(count);
	}
}
