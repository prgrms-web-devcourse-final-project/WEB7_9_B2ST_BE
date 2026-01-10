package com.back.b2st.domain.queue.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class QueueMetrics {
	private final MeterRegistry registry;
	private final Map<Long, AtomicInteger> queueWaitingCounts = new ConcurrentHashMap<>();
	private final Map<Long, AtomicInteger> queueEnterableCounts = new ConcurrentHashMap<>();

	public QueueMetrics(MeterRegistry registry) {
		this.registry = registry;
	}

	/** 대기열 진입 기록 */
	public void recordQueueEnter(Long queueId) {
		Counter.builder("queue_enter_total").tag("queue_id", String.valueOf(queueId)).register(registry).increment();
		getWaitingCount(queueId).incrementAndGet();
	}

	/** 대기열 이탈 기록 (COMPLETED/EXPIRED/CANCELLED) */
	public void recordQueueExit(Long queueId, String reason) {
		Counter.builder("queue_exit_total")
			.tag("queue_id", String.valueOf(queueId))
			.tag("reason", reason) // COMPLETED, EXPIRED, CANCELLED
			.register(registry)
			.increment();
		getWaitingCount(queueId).decrementAndGet();
	}

	/** WAITING → ENTERABLE 승격 기록 */
	public void recordMoveToEnterable(Long queueId) {
		Counter.builder("queue_enterable_total")
			.tag("queue_id", String.valueOf(queueId))
			.register(registry)
			.increment();
		getWaitingCount(queueId).decrementAndGet();
		getEnterableCount(queueId).incrementAndGet();
	}

	/** 대기열 입장 완료 기록 */
	public void recordEntryComplete(Long queueId) {
		Counter.builder("queue_complete_total").tag("queue_id", String.valueOf(queueId)).register(registry).increment();
		getEnterableCount(queueId).decrementAndGet();
	}

	/** 대기열 통계 갱신 (배치용) */
	public void updateQueueStats(Long queueId, int waiting, int enterable) {
		getWaitingCount(queueId).set(waiting);
		getEnterableCount(queueId).set(enterable);
	}

	private AtomicInteger getWaitingCount(Long queueId) {
		return queueWaitingCounts.computeIfAbsent(queueId, id -> {
			AtomicInteger count = new AtomicInteger(0);
			Gauge.builder("queue_waiting_count", count, AtomicInteger::get)
				.tag("queue_id", String.valueOf(id))
				.description("대기열 대기 인원")
				.register(registry);
			return count;
		});
	}

	private AtomicInteger getEnterableCount(Long queueId) {
		return queueEnterableCounts.computeIfAbsent(queueId, id -> {
			AtomicInteger count = new AtomicInteger(0);
			Gauge.builder("queue_enterable_count", count, AtomicInteger::get)
				.tag("queue_id", String.valueOf(id))
				.description("입장 가능 인원")
				.register(registry);
			return count;
		});
	}
}
