package com.back.b2st.domain.lottery.metrics;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class LotteryMetrics {
	private final MeterRegistry registry;

	public LotteryMetrics(MeterRegistry registry) {
		this.registry = registry;
	}

	/** 추첨 응모 기록 */
	public void recordEntry(Long performanceId, int quantity) {
		Counter.builder("lottery_entry_total")
				.tag("performance_id", String.valueOf(performanceId))
				.register(registry)
				.increment();
		Counter.builder("lottery_entry_quantity_total")
				.tag("performance_id", String.valueOf(performanceId))
				.register(registry)
				.increment(quantity);
	}

	/** 추첨 결과 기록 (당첨/낙첨) */
	public void recordDrawResult(Long performanceId, boolean won) {
		Counter.builder("lottery_draw_total")
				.tag("performance_id", String.valueOf(performanceId))
				.tag("result", won ? "won" : "lost")
				.register(registry)
				.increment();
	}

	/** 당첨자 결제 처리 기록 (완료/만료) */
	public void recordWinnerPayment(Long performanceId, boolean completed) {
		Counter.builder("lottery_winner_payment_total")
				.tag("performance_id", String.valueOf(performanceId))
				.tag("status", completed ? "completed" : "expired")
				.register(registry)
				.increment();
	}
}
