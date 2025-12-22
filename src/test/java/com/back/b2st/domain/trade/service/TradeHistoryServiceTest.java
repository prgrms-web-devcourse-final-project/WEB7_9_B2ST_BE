package com.back.b2st.domain.trade.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.back.b2st.domain.trade.dto.response.TransferTradeHistoryRes;
import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.entity.TradeType;
import com.back.b2st.domain.trade.repository.TradeRepository;

@ExtendWith(MockitoExtension.class)
class TradeHistoryServiceTest {

	@InjectMocks
	private TradeHistoryService tradeHistoryService;

	@Mock
	private TradeRepository tradeRepository;

	@Test
	@DisplayName("내 양도 구매 내역 조회 - buyerId 기준 조회 및 DTO 매핑")
	void getMyTransferPurchases_success() {
		// given
		Long memberId = 10L;
		LocalDateTime purchasedAt = LocalDateTime.of(2025, 12, 22, 12, 0);
		LocalDateTime createdAt = LocalDateTime.of(2025, 12, 22, 11, 0);

		Trade trade = Trade.builder()
			.memberId(1L)
			.performanceId(100L)
			.scheduleId(200L)
			.ticketId(300L)
			.type(TradeType.TRANSFER)
			.price(15000)
			.totalCount(1)
			.section("A")
			.row("5열")
			.seatNumber("12석")
			.build();
		ReflectionTestUtils.setField(trade, "id", 999L);
		ReflectionTestUtils.setField(trade, "buyerId", memberId);
		ReflectionTestUtils.setField(trade, "purchasedAt", purchasedAt);
		ReflectionTestUtils.setField(trade, "createdAt", createdAt);
		trade.complete();

		given(tradeRepository.findAllByBuyerIdAndTypeAndStatusOrderByPurchasedAtDesc(
			memberId, TradeType.TRANSFER, TradeStatus.COMPLETED
		)).willReturn(List.of(trade));

		// when
		List<TransferTradeHistoryRes> result = tradeHistoryService.getMyTransferPurchases(memberId);

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).tradeId()).isEqualTo(999L);
		assertThat(result.get(0).type()).isEqualTo(TradeType.TRANSFER);
		assertThat(result.get(0).status()).isEqualTo(TradeStatus.COMPLETED);
		assertThat(result.get(0).price()).isEqualTo(15000);
		assertThat(result.get(0).performanceId()).isEqualTo(100L);
		assertThat(result.get(0).scheduleId()).isEqualTo(200L);
		assertThat(result.get(0).totalCount()).isEqualTo(1);
		assertThat(result.get(0).section()).isEqualTo("A");
		assertThat(result.get(0).row()).isEqualTo("5열");
		assertThat(result.get(0).seatNumber()).isEqualTo("12석");
		assertThat(result.get(0).purchasedAt()).isEqualTo(purchasedAt);
		assertThat(result.get(0).createdAt()).isEqualTo(createdAt);

		verify(tradeRepository).findAllByBuyerIdAndTypeAndStatusOrderByPurchasedAtDesc(
			memberId, TradeType.TRANSFER, TradeStatus.COMPLETED
		);
	}

	@Test
	@DisplayName("내 양도 판매 내역 조회 - memberId 기준 조회 및 DTO 매핑")
	void getMyTransferSales_success() {
		// given
		Long memberId = 10L;
		LocalDateTime purchasedAt = LocalDateTime.of(2025, 12, 22, 12, 0);
		LocalDateTime createdAt = LocalDateTime.of(2025, 12, 22, 11, 0);

		Trade trade = Trade.builder()
			.memberId(memberId)
			.performanceId(100L)
			.scheduleId(200L)
			.ticketId(300L)
			.type(TradeType.TRANSFER)
			.price(15000)
			.totalCount(1)
			.section("A")
			.row("5열")
			.seatNumber("12석")
			.build();
		ReflectionTestUtils.setField(trade, "id", 999L);
		ReflectionTestUtils.setField(trade, "buyerId", 20L);
		ReflectionTestUtils.setField(trade, "purchasedAt", purchasedAt);
		ReflectionTestUtils.setField(trade, "createdAt", createdAt);
		trade.complete();

		given(tradeRepository.findAllByMemberIdAndTypeAndStatusOrderByPurchasedAtDesc(
			memberId, TradeType.TRANSFER, TradeStatus.COMPLETED
		)).willReturn(List.of(trade));

		// when
		List<TransferTradeHistoryRes> result = tradeHistoryService.getMyTransferSales(memberId);

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).tradeId()).isEqualTo(999L);
		assertThat(result.get(0).type()).isEqualTo(TradeType.TRANSFER);
		assertThat(result.get(0).status()).isEqualTo(TradeStatus.COMPLETED);
		assertThat(result.get(0).price()).isEqualTo(15000);
		assertThat(result.get(0).performanceId()).isEqualTo(100L);
		assertThat(result.get(0).scheduleId()).isEqualTo(200L);
		assertThat(result.get(0).totalCount()).isEqualTo(1);
		assertThat(result.get(0).section()).isEqualTo("A");
		assertThat(result.get(0).row()).isEqualTo("5열");
		assertThat(result.get(0).seatNumber()).isEqualTo("12석");
		assertThat(result.get(0).purchasedAt()).isEqualTo(purchasedAt);
		assertThat(result.get(0).createdAt()).isEqualTo(createdAt);

		verify(tradeRepository).findAllByMemberIdAndTypeAndStatusOrderByPurchasedAtDesc(
			memberId, TradeType.TRANSFER, TradeStatus.COMPLETED
		);
	}

	@Test
	@DisplayName("양도 구매/판매 내역 응답은 개인정보(buyerId/memberId)를 포함하지 않는다")
	void transferTradeHistoryRes_noPiiComponents() {
		List<String> componentNames = Arrays.stream(TransferTradeHistoryRes.class.getRecordComponents())
			.map(component -> component.getName())
			.toList();
		assertThat(componentNames).doesNotContain("buyerId", "memberId", "sellerId", "nickname", "name");
	}
}
