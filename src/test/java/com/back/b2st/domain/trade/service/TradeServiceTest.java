package com.back.b2st.domain.trade.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.back.b2st.domain.trade.dto.request.CreateTradeRequest;
import com.back.b2st.domain.trade.dto.response.CreateTradeResponse;
import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.entity.TradeType;
import com.back.b2st.domain.trade.error.TradeErrorCode;
import com.back.b2st.domain.trade.repository.TradeRepository;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

	@InjectMocks
	private TradeService tradeService;

	@Mock
	private TradeRepository tradeRepository;

	@Test
	@DisplayName("교환 게시글 생성 성공")
	void createExchangeTrade_success() {
		// given
		CreateTradeRequest request = new CreateTradeRequest(1L, TradeType.EXCHANGE, null, 1);
		Long memberId = 100L;

		given(tradeRepository.existsByTicketIdAndStatus(1L, TradeStatus.ACTIVE))
			.willReturn(false);

		Trade mockTrade = Trade.builder()
			.memberId(memberId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(1L)
			.type(TradeType.EXCHANGE)
			.price(null)
			.totalCount(1)
			.section("A")
			.row("5열")
			.seatNumber("12석")
			.build();

		given(tradeRepository.save(any(Trade.class))).willReturn(mockTrade);

		// when
		CreateTradeResponse response = tradeService.createTrade(request, memberId);

		// then
		assertThat(response.getType()).isEqualTo(TradeType.EXCHANGE);
		assertThat(response.getTotalCount()).isEqualTo(1);
		assertThat(response.getPrice()).isNull();
		verify(tradeRepository).save(any(Trade.class));
	}

	@Test
	@DisplayName("양도 게시글 생성 성공")
	void createTransferTrade_success() {
		// given
		CreateTradeRequest request = new CreateTradeRequest(1L, TradeType.TRANSFER, 50000, 2);
		Long memberId = 100L;

		given(tradeRepository.existsByTicketIdAndStatus(1L, TradeStatus.ACTIVE))
			.willReturn(false);

		Trade mockTrade = Trade.builder()
			.memberId(memberId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(1L)
			.type(TradeType.TRANSFER)
			.price(50000)
			.totalCount(2)
			.section("A")
			.row("5열")
			.seatNumber("12석")
			.build();

		given(tradeRepository.save(any(Trade.class))).willReturn(mockTrade);

		// when
		CreateTradeResponse response = tradeService.createTrade(request, memberId);

		// then
		assertThat(response.getType()).isEqualTo(TradeType.TRANSFER);
		assertThat(response.getPrice()).isEqualTo(50000);
		assertThat(response.getTotalCount()).isEqualTo(2);
		verify(tradeRepository).save(any(Trade.class));
	}

	@Test
	@DisplayName("티켓 중복 등록 실패")
	void createTrade_fail_duplicateTicket() {
		// given
		CreateTradeRequest request = new CreateTradeRequest(1L, TradeType.EXCHANGE, null, 1);
		Long memberId = 100L;

		given(tradeRepository.existsByTicketIdAndStatus(1L, TradeStatus.ACTIVE))
			.willReturn(true);

		// when & then
		assertThatThrownBy(() -> tradeService.createTrade(request, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.TICKET_ALREADY_REGISTERED);
	}

	@Test
	@DisplayName("교환 - totalCount 검증 실패 (1개 초과)")
	void createExchangeTrade_fail_invalidCount() {
		// given
		CreateTradeRequest request = new CreateTradeRequest(1L, TradeType.EXCHANGE, null, 2);
		Long memberId = 100L;

		given(tradeRepository.existsByTicketIdAndStatus(1L, TradeStatus.ACTIVE))
			.willReturn(false);

		// when & then
		assertThatThrownBy(() -> tradeService.createTrade(request, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_EXCHANGE_COUNT);
	}

	@Test
	@DisplayName("교환 - price 검증 실패 (가격 설정)")
	void createExchangeTrade_fail_invalidPrice() {
		// given
		CreateTradeRequest request = new CreateTradeRequest(1L, TradeType.EXCHANGE, 10000, 1);
		Long memberId = 100L;

		given(tradeRepository.existsByTicketIdAndStatus(1L, TradeStatus.ACTIVE))
			.willReturn(false);

		// when & then
		assertThatThrownBy(() -> tradeService.createTrade(request, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_EXCHANGE_PRICE);
	}

	@Test
	@DisplayName("양도 - price 검증 실패 (가격 미설정)")
	void createTransferTrade_fail_noPrice() {
		// given
		CreateTradeRequest request = new CreateTradeRequest(1L, TradeType.TRANSFER, null, 1);
		Long memberId = 100L;

		given(tradeRepository.existsByTicketIdAndStatus(1L, TradeStatus.ACTIVE))
			.willReturn(false);

		// when & then
		assertThatThrownBy(() -> tradeService.createTrade(request, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_TRANSFER_PRICE);
	}

	@Test
	@DisplayName("양도 - price 검증 실패 (0원 이하)")
	void createTransferTrade_fail_invalidPrice() {
		// given
		CreateTradeRequest request = new CreateTradeRequest(1L, TradeType.TRANSFER, 0, 1);
		Long memberId = 100L;

		given(tradeRepository.existsByTicketIdAndStatus(1L, TradeStatus.ACTIVE))
			.willReturn(false);

		// when & then
		assertThatThrownBy(() -> tradeService.createTrade(request, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_TRANSFER_PRICE);
	}

	@Test
	@DisplayName("동시성 문제로 중복 발생 시 예외 처리")
	void createTrade_fail_dataIntegrityViolation() {
		// given
		CreateTradeRequest request = new CreateTradeRequest(1L, TradeType.EXCHANGE, null, 1);
		Long memberId = 100L;

		given(tradeRepository.existsByTicketIdAndStatus(1L, TradeStatus.ACTIVE))
			.willReturn(false);

		given(tradeRepository.save(any(Trade.class)))
			.willThrow(new DataIntegrityViolationException("Unique constraint violation"));

		// when & then
		assertThatThrownBy(() -> tradeService.createTrade(request, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.TICKET_ALREADY_REGISTERED);
	}
}
