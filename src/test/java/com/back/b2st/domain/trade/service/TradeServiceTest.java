package com.back.b2st.domain.trade.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.repository.TicketRepository;
import com.back.b2st.domain.trade.dto.request.CreateTradeReq;
import com.back.b2st.domain.trade.dto.request.UpdateTradeReq;
import com.back.b2st.domain.trade.dto.response.CreateTradeRes;
import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeRequest;
import com.back.b2st.domain.trade.entity.TradeRequestStatus;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.entity.TradeType;
import com.back.b2st.domain.trade.error.TradeErrorCode;
import com.back.b2st.domain.trade.repository.TradeRepository;
import com.back.b2st.domain.trade.repository.TradeRequestRepository;
import com.back.b2st.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

	@InjectMocks
	private TradeService tradeService;

	@Mock
	private TradeRepository tradeRepository;

	@Mock
	private TradeRequestRepository tradeRequestRepository;

	@Mock
	private TicketRepository ticketRepository;

	@Mock
	private SeatRepository seatRepository;

	@Mock
	private ReservationRepository reservationRepository;

	@Test
	@DisplayName("교환 게시글 생성 성공")
	void createExchangeTrade_success() {
		// given
		CreateTradeReq request = new CreateTradeReq(java.util.List.of(1L), TradeType.EXCHANGE, null);
		Long memberId = 100L;

		given(tradeRepository.existsByTicketIdAndStatus(1L, TradeStatus.ACTIVE))
			.willReturn(false);

		Ticket mockTicket = Ticket.builder()
			.reservationId(1L)
			.memberId(memberId)
			.seatId(1L)
			.qrCode("QR123")
			.build();

		Seat mockSeat = Seat.builder()
			.venueId(1L)
			.sectionId(1L)
			.sectionName("A구역")
			.rowLabel("5열")
			.seatNumber(12)
			.build();

		Reservation mockReservation = Reservation.builder()
			.performanceId(1L)
			.memberId(memberId)
			.seatId(1L)
			.build();

		given(ticketRepository.findById(1L)).willReturn(Optional.of(mockTicket));
		given(seatRepository.findById(1L)).willReturn(Optional.of(mockSeat));
		given(reservationRepository.findById(1L)).willReturn(Optional.of(mockReservation));

		Trade mockTrade = Trade.builder()
			.memberId(memberId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(1L)
			.type(TradeType.EXCHANGE)
			.price(null)
			.totalCount(1)
			.section("A구역")
			.row("5열")
			.seatNumber("12")
			.build();

		given(tradeRepository.save(any(Trade.class))).willReturn(mockTrade);

		// when
		List<CreateTradeRes> response = tradeService.createTrade(request, memberId);

		// then
		assertThat(response).hasSize(1);
		assertThat(response.get(0).type()).isEqualTo(TradeType.EXCHANGE);
		assertThat(response.get(0).totalCount()).isEqualTo(1);
		assertThat(response.get(0).price()).isNull();
		verify(tradeRepository).save(any(Trade.class));
	}

	@Test
	@DisplayName("양도 게시글 생성 성공")
	void createTransferTrade_success() {
		// given
		CreateTradeReq request = new CreateTradeReq(java.util.List.of(1L), TradeType.TRANSFER, 50000);
		Long memberId = 100L;

		given(tradeRepository.existsByTicketIdAndStatus(1L, TradeStatus.ACTIVE))
			.willReturn(false);

		Ticket mockTicket = Ticket.builder()
			.reservationId(1L)
			.memberId(memberId)
			.seatId(1L)
			.qrCode("QR123")
			.build();

		Seat mockSeat = Seat.builder()
			.venueId(1L)
			.sectionId(1L)
			.sectionName("A구역")
			.rowLabel("5열")
			.seatNumber(12)
			.build();

		Reservation mockReservation = Reservation.builder()
			.performanceId(1L)
			.memberId(memberId)
			.seatId(1L)
			.build();

		given(ticketRepository.findById(1L)).willReturn(Optional.of(mockTicket));
		given(seatRepository.findById(1L)).willReturn(Optional.of(mockSeat));
		given(reservationRepository.findById(1L)).willReturn(Optional.of(mockReservation));

		Trade mockTrade = Trade.builder()
			.memberId(memberId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(1L)
			.type(TradeType.TRANSFER)
			.price(50000)
			.totalCount(1)
			.section("A구역")
			.row("5열")
			.seatNumber("12")
			.build();

		given(tradeRepository.save(any(Trade.class))).willReturn(mockTrade);

		// when
		List<CreateTradeRes> response = tradeService.createTrade(request, memberId);

		// then
		assertThat(response).hasSize(1);
		assertThat(response.get(0).type()).isEqualTo(TradeType.TRANSFER);
		assertThat(response.get(0).price()).isEqualTo(50000);
		assertThat(response.get(0).totalCount()).isEqualTo(1);
		verify(tradeRepository).save(any(Trade.class));
	}

	@Test
	@DisplayName("티켓 중복 등록 실패")
	void createTrade_fail_duplicateTicket() {
		// given
		CreateTradeReq request = new CreateTradeReq(java.util.List.of(1L), TradeType.EXCHANGE, null);
		Long memberId = 100L;

		given(tradeRepository.existsByTicketIdAndStatus(1L, TradeStatus.ACTIVE))
			.willReturn(true);

		// when & then
		assertThatThrownBy(() -> tradeService.createTrade(request, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("교환 - price 검증 실패 (가격 설정)")
	void createExchangeTrade_fail_invalidPrice() {
		// given
		CreateTradeReq request = new CreateTradeReq(java.util.List.of(1L), TradeType.EXCHANGE, 10000);
		Long memberId = 100L;

		// when & then
		assertThatThrownBy(() -> tradeService.createTrade(request, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("양도 - price 검증 실패 (가격 미설정)")
	void createTransferTrade_fail_noPrice() {
		// given
		CreateTradeReq request = new CreateTradeReq(java.util.List.of(1L), TradeType.TRANSFER, null);
		Long memberId = 100L;

		// when & then
		assertThatThrownBy(() -> tradeService.createTrade(request, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("양도 - price 검증 실패 (0원 이하)")
	void createTransferTrade_fail_invalidPrice() {
		// given
		CreateTradeReq request = new CreateTradeReq(java.util.List.of(1L), TradeType.TRANSFER, 0);
		Long memberId = 100L;

		// when & then
		assertThatThrownBy(() -> tradeService.createTrade(request, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("동시성 문제로 중복 발생 시 예외 처리")
	void createTrade_fail_dataIntegrityViolation() {
		// given
		CreateTradeReq request = new CreateTradeReq(java.util.List.of(1L), TradeType.EXCHANGE, null);
		Long memberId = 100L;

		given(tradeRepository.existsByTicketIdAndStatus(1L, TradeStatus.ACTIVE))
			.willReturn(false);

		Ticket mockTicket = Ticket.builder()
			.reservationId(1L)
			.memberId(memberId)
			.seatId(1L)
			.qrCode("QR123")
			.build();

		Seat mockSeat = Seat.builder()
			.venueId(1L)
			.sectionId(1L)
			.sectionName("A구역")
			.rowLabel("5열")
			.seatNumber(12)
			.build();

		Reservation mockReservation = Reservation.builder()
			.performanceId(1L)
			.memberId(memberId)
			.seatId(1L)
			.build();

		given(ticketRepository.findById(1L)).willReturn(Optional.of(mockTicket));
		given(seatRepository.findById(1L)).willReturn(Optional.of(mockSeat));
		given(reservationRepository.findById(1L)).willReturn(Optional.of(mockReservation));

		given(tradeRepository.save(any(Trade.class)))
			.willThrow(new DataIntegrityViolationException("Unique constraint violation"));

		// when & then
		assertThatThrownBy(() -> tradeService.createTrade(request, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("type과 status로 필터링하여 거래 목록 조회")
	void getTrades_withTypeAndStatus() {
		// given
		TradeType type = TradeType.EXCHANGE;
		TradeStatus status = TradeStatus.ACTIVE;
		org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

		Trade trade = Trade.builder()
			.memberId(100L)
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

		org.springframework.data.domain.Page<Trade> tradePage =
			new org.springframework.data.domain.PageImpl<>(List.of(trade));

		given(tradeRepository.findAllByTypeAndStatus(type, status, pageable))
			.willReturn(tradePage);

		// when
		org.springframework.data.domain.Page<com.back.b2st.domain.trade.dto.response.TradeRes> result =
			tradeService.getTrades(type, status, pageable);

		// then
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).type()).isEqualTo(TradeType.EXCHANGE);
		verify(tradeRepository).findAllByTypeAndStatus(type, status, pageable);
	}

	@Test
	@DisplayName("양도 게시글 수정 성공")
	void updateTrade_success() {
		// given
		Long tradeId = 1L;
		Long memberId = 100L;
		UpdateTradeReq request = new UpdateTradeReq(60000);

		Trade trade = Trade.builder()
			.memberId(memberId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(1L)
			.type(TradeType.TRANSFER)
			.price(50000)
			.totalCount(1)
			.section("A")
			.row("5열")
			.seatNumber("12석")
			.build();

		given(tradeRepository.findById(tradeId)).willReturn(Optional.of(trade));

		// when
		tradeService.updateTrade(tradeId, request, memberId);

		// then
		assertThat(trade.getPrice()).isEqualTo(60000);
	}

	@Test
	@DisplayName("존재하지 않는 Trade ID로 수정 실패")
	void updateTrade_fail_notFound() {
		// given
		Long tradeId = 999L;
		Long memberId = 100L;
		UpdateTradeReq request = new UpdateTradeReq(60000);

		given(tradeRepository.findById(tradeId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> tradeService.updateTrade(tradeId, request, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.TRADE_NOT_FOUND);
	}

	@Test
	@DisplayName("본인이 아닌 경우 수정 실패")
	void updateTrade_fail_unauthorized() {
		// given
		Long tradeId = 1L;
		Long ownerId = 100L;
		Long requesterId = 200L;
		UpdateTradeReq request = new UpdateTradeReq(60000);

		Trade trade = Trade.builder()
			.memberId(ownerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(1L)
			.type(TradeType.TRANSFER)
			.price(50000)
			.totalCount(1)
			.section("A")
			.row("5열")
			.seatNumber("12석")
			.build();

		given(tradeRepository.findById(tradeId)).willReturn(Optional.of(trade));

		// when & then
		assertThatThrownBy(() -> tradeService.updateTrade(tradeId, request, requesterId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.UNAUTHORIZED_TRADE_ACCESS);
	}

	@Test
	@DisplayName("ACTIVE가 아닌 상태에서 수정 실패")
	void updateTrade_fail_notActive() {
		// given
		Long tradeId = 1L;
		Long memberId = 100L;
		UpdateTradeReq request = new UpdateTradeReq(60000);

		Trade trade = Trade.builder()
			.memberId(memberId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(1L)
			.type(TradeType.TRANSFER)
			.price(50000)
			.totalCount(1)
			.section("A")
			.row("5열")
			.seatNumber("12석")
			.build();
		trade.cancel();

		given(tradeRepository.findById(tradeId)).willReturn(Optional.of(trade));

		// when & then
		assertThatThrownBy(() -> tradeService.updateTrade(tradeId, request, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("교환 타입 수정 실패")
	void updateTrade_fail_exchangeType() {
		// given
		Long tradeId = 1L;
		Long memberId = 100L;
		UpdateTradeReq request = new UpdateTradeReq(60000);

		Trade trade = Trade.builder()
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

		given(tradeRepository.findById(tradeId)).willReturn(Optional.of(trade));

		// when & then
		assertThatThrownBy(() -> tradeService.updateTrade(tradeId, request, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("게시글 삭제 성공")
	void deleteTrade_success() {
		// given
		Long tradeId = 1L;
		Long memberId = 100L;

		Trade trade = Trade.builder()
			.memberId(memberId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(1L)
			.type(TradeType.TRANSFER)
			.price(50000)
			.totalCount(1)
			.section("A")
			.row("5열")
			.seatNumber("12석")
			.build();

		given(tradeRepository.findById(tradeId)).willReturn(Optional.of(trade));
		given(tradeRequestRepository.findByTradeAndStatus(trade, TradeRequestStatus.PENDING))
			.willReturn(Collections.emptyList());

		// when
		tradeService.deleteTrade(tradeId, memberId);

		// then
		assertThat(trade.getStatus()).isEqualTo(TradeStatus.CANCELLED);
		assertThat(trade.getDeletedAt()).isNotNull();
	}

	@Test
	@DisplayName("존재하지 않는 Trade ID로 삭제 실패")
	void deleteTrade_fail_notFound() {
		// given
		Long tradeId = 999L;
		Long memberId = 100L;

		given(tradeRepository.findById(tradeId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> tradeService.deleteTrade(tradeId, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.TRADE_NOT_FOUND);
	}

	@Test
	@DisplayName("본인이 아닌 경우 삭제 실패")
	void deleteTrade_fail_unauthorized() {
		// given
		Long tradeId = 1L;
		Long ownerId = 100L;
		Long requesterId = 200L;

		Trade trade = Trade.builder()
			.memberId(ownerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(1L)
			.type(TradeType.TRANSFER)
			.price(50000)
			.totalCount(1)
			.section("A")
			.row("5열")
			.seatNumber("12석")
			.build();

		given(tradeRepository.findById(tradeId)).willReturn(Optional.of(trade));

		// when & then
		assertThatThrownBy(() -> tradeService.deleteTrade(tradeId, requesterId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.UNAUTHORIZED_TRADE_ACCESS);
	}

	@Test
	@DisplayName("ACTIVE가 아닌 상태에서 삭제 실패")
	void deleteTrade_fail_notActive() {
		// given
		Long tradeId = 1L;
		Long memberId = 100L;

		Trade trade = Trade.builder()
			.memberId(memberId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(1L)
			.type(TradeType.TRANSFER)
			.price(50000)
			.totalCount(1)
			.section("A")
			.row("5열")
			.seatNumber("12석")
			.build();
		trade.complete();

		given(tradeRepository.findById(tradeId)).willReturn(Optional.of(trade));

		// when & then
		assertThatThrownBy(() -> tradeService.deleteTrade(tradeId, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("PENDING 교환 신청이 있는 경우 삭제 실패")
	void deleteTrade_fail_hasPendingRequests() {
		// given
		Long tradeId = 1L;
		Long memberId = 100L;

		Trade trade = Trade.builder()
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

		TradeRequest pendingRequest = TradeRequest.builder()
			.trade(trade)
			.requesterId(200L)
			.requesterTicketId(2L)
			.build();

		given(tradeRepository.findById(tradeId)).willReturn(Optional.of(trade));
		given(tradeRequestRepository.findByTradeAndStatus(trade, TradeRequestStatus.PENDING))
			.willReturn(List.of(pendingRequest));

		// when & then
		assertThatThrownBy(() -> tradeService.deleteTrade(tradeId, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("교환 - 티켓이 2개 이상일 때 실패")
	void createExchangeTrade_fail_moreThanOneTicket() {
		// given
		CreateTradeReq request = new CreateTradeReq(
			java.util.List.of(1L, 2L),
			TradeType.EXCHANGE,
			null
		);
		Long memberId = 100L;

		// when & then
		assertThatThrownBy(() -> tradeService.createTrade(request, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("양도 - 티켓이 0개일 때 실패")
	void createTransferTrade_fail_emptyTickets() {
		// given
		CreateTradeReq request = new CreateTradeReq(
			Collections.emptyList(),
			TradeType.TRANSFER,
			50000
		);
		Long memberId = 100L;

		// when & then
		assertThatThrownBy(() -> tradeService.createTrade(request, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("다른 사용자의 티켓으로 거래 생성 실패")
	void createTrade_fail_notOwnedTicket() {
		// given
		CreateTradeReq request = new CreateTradeReq(
			java.util.List.of(1L),
			TradeType.TRANSFER,
			50000
		);
		Long memberId = 100L;
		Long otherMemberId = 200L;

		given(tradeRepository.existsByTicketIdAndStatus(1L, TradeStatus.ACTIVE))
			.willReturn(false);

		Ticket mockTicket = Ticket.builder()
			.reservationId(1L)
			.memberId(otherMemberId)
			.seatId(1L)
			.qrCode("QR123")
			.build();

		given(ticketRepository.findById(1L)).willReturn(Optional.of(mockTicket));

		// when & then
		assertThatThrownBy(() -> tradeService.createTrade(request, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_REQUEST);
	}
}
