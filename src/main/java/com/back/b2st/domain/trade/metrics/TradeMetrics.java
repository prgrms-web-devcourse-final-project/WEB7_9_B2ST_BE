package com.back.b2st.domain.trade.metrics;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.back.b2st.domain.trade.entity.TradeType;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class TradeMetrics {
	private final MeterRegistry registry;
	private final AtomicInteger activeTradeCount = new AtomicInteger(0);
	private final DistributionSummary tradePriceSummary;

	public TradeMetrics(MeterRegistry registry) {
		this.registry = registry;
		Gauge.builder("trade_active_count", activeTradeCount, AtomicInteger::get)
				.description("현재 활성 거래 수")
				.register(registry);
		this.tradePriceSummary = DistributionSummary.builder("trade_price")
				.description("양도 금액 분포")
				.baseUnit("won")
				.publishPercentiles(0.5, 0.95)
				.register(registry);
	}

	/** 거래 생성 기록 (양도/교환) */
	public void recordTradeCreated(TradeType type, Integer price) {
		Counter.builder("trade_total")
				.tag("action", "created")
				.tag("type", type.name())
				.register(registry)
				.increment();
		activeTradeCount.incrementAndGet();
		if (type == TradeType.TRANSFER && price != null) {
			tradePriceSummary.record(price);
		}
	}

	/** 거래 완료 기록 */
	public void recordTradeCompleted(TradeType type) {
		Counter.builder("trade_total")
				.tag("action", "completed")
				.tag("type", type.name())
				.register(registry)
				.increment();
		activeTradeCount.decrementAndGet();
	}

	/** 거래 취소 기록 */
	public void recordTradeCancelled(TradeType type) {
		Counter.builder("trade_total")
				.tag("action", "cancelled")
				.tag("type", type.name())
				.register(registry)
				.increment();
		activeTradeCount.decrementAndGet();
	}

	/** 거래 요청 기록 */
	public void recordTradeRequest(TradeType type) {
		Counter.builder("trade_request_total")
				.tag("type", type.name())
				.register(registry)
				.increment();
	}

	/** 활성 거래 수 설정 (배치용) */
	public void setActiveTradeCount(int count) {
		activeTradeCount.set(count);
	}
}
