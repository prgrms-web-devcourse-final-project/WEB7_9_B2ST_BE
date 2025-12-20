package com.back.b2st.domain.trade.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.entity.TicketStatus;
import com.back.b2st.domain.ticket.error.TicketErrorCode;
import com.back.b2st.domain.ticket.service.TicketService;
import com.back.b2st.domain.trade.dto.request.CreateTradeRequestReq;
import com.back.b2st.domain.trade.dto.response.TradeRequestRes;
import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeRequest;
import com.back.b2st.domain.trade.entity.TradeRequestStatus;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.entity.TradeType;
import com.back.b2st.domain.trade.error.TradeErrorCode;
import com.back.b2st.domain.trade.repository.TradeRepository;
import com.back.b2st.domain.trade.repository.TradeRequestRepository;
import com.back.b2st.global.error.exception.BusinessException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@ExtendWith(MockitoExtension.class)
class TradeRequestServiceTest {

	@InjectMocks
	private TradeRequestService tradeRequestService;

	@Mock
	private TradeRequestRepository tradeRequestRepository;

	@Mock
	private TradeRepository tradeRepository;

	@Mock
	private TicketService ticketService;

	@Mock
	private EntityManager entityManager;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(tradeRequestService, "entityManager", entityManager);
	}

	@Test
	@DisplayName("교환 신청 생성 성공")
	void createTradeRequest_success() {
		// given
		Long tradeId = 1L;
		Long requesterId = 200L;
		CreateTradeRequestReq request = new CreateTradeRequestReq(10L);

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

		TradeRequest tradeRequest = TradeRequest.builder()
			.trade(trade)
			.requesterId(requesterId)
			.requesterTicketId(request.requesterTicketId())
			.build();

		given(tradeRepository.findById(tradeId)).willReturn(Optional.of(trade));
		given(tradeRequestRepository.findByRequesterIdAndStatus(requesterId, TradeRequestStatus.PENDING))
			.willReturn(Collections.emptyList());
		given(tradeRequestRepository.save(any(TradeRequest.class))).willReturn(tradeRequest);

		// when
		TradeRequestRes response = tradeRequestService.createTradeRequest(tradeId, request, requesterId);

		// then
		assertThat(response).isNotNull();
		assertThat(response.requesterId()).isEqualTo(requesterId);
		assertThat(response.requesterTicketId()).isEqualTo(10L);
		assertThat(response.status()).isEqualTo(TradeRequestStatus.PENDING);
		verify(tradeRequestRepository).save(any(TradeRequest.class));
	}

	@Test
	@DisplayName("교환 신청 생성 실패 - 존재하지 않는 Trade")
	void createTradeRequest_fail_tradeNotFound() {
		// given
		Long tradeId = 999L;
		Long requesterId = 200L;
		CreateTradeRequestReq request = new CreateTradeRequestReq(10L);

		given(tradeRepository.findById(tradeId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> tradeRequestService.createTradeRequest(tradeId, request, requesterId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.TRADE_NOT_FOUND);
	}

	@Test
	@DisplayName("교환 신청 생성 실패 - ACTIVE 상태가 아닌 Trade")
	void createTradeRequest_fail_tradeNotActive() {
		// given
		Long tradeId = 1L;
		Long requesterId = 200L;
		CreateTradeRequestReq request = new CreateTradeRequestReq(10L);

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
		trade.complete();

		given(tradeRepository.findById(tradeId)).willReturn(Optional.of(trade));

		// when & then
		assertThatThrownBy(() -> tradeRequestService.createTradeRequest(tradeId, request, requesterId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("교환 신청 생성 실패 - 자신의 게시글")
	void createTradeRequest_fail_ownTrade() {
		// given
		Long tradeId = 1L;
		Long requesterId = 100L;
		CreateTradeRequestReq request = new CreateTradeRequestReq(10L);

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

		given(tradeRepository.findById(tradeId)).willReturn(Optional.of(trade));

		// when & then
		assertThatThrownBy(() -> tradeRequestService.createTradeRequest(tradeId, request, requesterId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("교환 신청 생성 실패 - 중복 신청")
	void createTradeRequest_fail_duplicate() throws Exception {
		// given
		Long tradeId = 1L;
		Long requesterId = 200L;
		CreateTradeRequestReq request = new CreateTradeRequestReq(10L);

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

		// Set ID using reflection
		Field idField = Trade.class.getDeclaredField("id");
		idField.setAccessible(true);
		idField.set(trade, tradeId);

		TradeRequest existingRequest = TradeRequest.builder()
			.trade(trade)
			.requesterId(requesterId)
			.requesterTicketId(5L)
			.build();

		given(tradeRepository.findById(tradeId)).willReturn(Optional.of(trade));
		given(tradeRequestRepository.findByRequesterIdAndStatus(requesterId, TradeRequestStatus.PENDING))
			.willReturn(List.of(existingRequest));

		// when & then
		assertThatThrownBy(() -> tradeRequestService.createTradeRequest(tradeId, request, requesterId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("교환 신청 조회 성공")
	void getTradeRequest_success() {
		// given
		Long tradeRequestId = 1L;
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

		TradeRequest tradeRequest = TradeRequest.builder()
			.trade(trade)
			.requesterId(200L)
			.requesterTicketId(10L)
			.build();

		given(tradeRequestRepository.findById(tradeRequestId)).willReturn(Optional.of(tradeRequest));

		// when
		TradeRequestRes response = tradeRequestService.getTradeRequest(tradeRequestId);

		// then
		assertThat(response).isNotNull();
		assertThat(response.requesterId()).isEqualTo(200L);
		assertThat(response.requesterTicketId()).isEqualTo(10L);
	}

	@Test
	@DisplayName("교환 신청 조회 실패 - 존재하지 않는 신청")
	void getTradeRequest_fail_notFound() {
		// given
		Long tradeRequestId = 999L;
		given(tradeRequestRepository.findById(tradeRequestId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> tradeRequestService.getTradeRequest(tradeRequestId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.TRADE_REQUEST_NOT_FOUND);
	}

	@Test
	@DisplayName("Trade별 신청 목록 조회 성공")
	void getTradeRequestsByTrade_success() {
		// given
		Long tradeId = 1L;
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

		TradeRequest request1 = TradeRequest.builder()
			.trade(trade)
			.requesterId(200L)
			.requesterTicketId(10L)
			.build();

		TradeRequest request2 = TradeRequest.builder()
			.trade(trade)
			.requesterId(201L)
			.requesterTicketId(11L)
			.build();

		given(tradeRepository.findById(tradeId)).willReturn(Optional.of(trade));
		given(tradeRequestRepository.findByTrade(trade)).willReturn(List.of(request1, request2));

		// when
		List<TradeRequestRes> responses = tradeRequestService.getTradeRequestsByTrade(tradeId);

		// then
		assertThat(responses).hasSize(2);
		assertThat(responses.get(0).requesterId()).isEqualTo(200L);
		assertThat(responses.get(1).requesterId()).isEqualTo(201L);
	}

	@Test
	@DisplayName("신청자별 신청 목록 조회 성공")
	void getTradeRequestsByRequester_success() {
		// given
		Long requesterId = 200L;
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

		TradeRequest request1 = TradeRequest.builder()
			.trade(trade)
			.requesterId(requesterId)
			.requesterTicketId(10L)
			.build();

		given(tradeRequestRepository.findByRequesterId(requesterId)).willReturn(List.of(request1));

		// when
		List<TradeRequestRes> responses = tradeRequestService.getTradeRequestsByRequester(requesterId);

		// then
		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).requesterId()).isEqualTo(requesterId);
	}

	@Test
	@DisplayName("교환 신청 수락 성공")
	void acceptTradeRequest_success() {
		// given
		Long tradeRequestId = 1L;
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

		TradeRequest tradeRequest = TradeRequest.builder()
			.trade(trade)
			.requesterId(200L)
			.requesterTicketId(10L)
			.build();

		Ticket ownerTicket = Ticket.builder()
			.reservationId(1L)
			.memberId(memberId)
			.seatId(1L)
			.build();

		Ticket requesterTicket = Ticket.builder()
			.reservationId(2L)
			.memberId(200L)
			.seatId(10L)
			.build();

		given(entityManager.find(TradeRequest.class, tradeRequestId, LockModeType.PESSIMISTIC_WRITE)).willReturn(tradeRequest);
		given(entityManager.find(Trade.class, trade.getId(), LockModeType.PESSIMISTIC_WRITE)).willReturn(trade);
		given(tradeRequestRepository.findByTradeAndStatus(trade, TradeRequestStatus.ACCEPTED))
			.willReturn(Collections.emptyList());
		given(ticketService.getTicketById(1L)).willReturn(ownerTicket);
		given(ticketService.getTicketById(10L)).willReturn(requesterTicket);
		given(ticketService.exchangeTicket(anyLong(), anyLong(), anyLong())).willReturn(ownerTicket);
		given(ticketService.createTicket(anyLong(), anyLong(), anyLong())).willReturn(ownerTicket);

		// when
		tradeRequestService.acceptTradeRequest(tradeRequestId, memberId);

		// then
		assertThat(tradeRequest.getStatus()).isEqualTo(TradeRequestStatus.ACCEPTED);
		assertThat(trade.getStatus()).isEqualTo(TradeStatus.COMPLETED);
		verify(ticketService, times(2)).getTicketById(anyLong());
		verify(ticketService, times(2)).exchangeTicket(anyLong(), anyLong(), anyLong());
		verify(ticketService, times(2)).createTicket(anyLong(), anyLong(), anyLong());
	}

	@Test
	@DisplayName("교환 신청 수락 실패 - 권한 없음")
	void acceptTradeRequest_fail_unauthorized() {
		// given
		Long tradeRequestId = 1L;
		Long memberId = 999L;

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

		TradeRequest tradeRequest = TradeRequest.builder()
			.trade(trade)
			.requesterId(200L)
			.requesterTicketId(10L)
			.build();

		given(entityManager.find(TradeRequest.class, tradeRequestId, LockModeType.PESSIMISTIC_WRITE)).willReturn(tradeRequest);
		given(entityManager.find(Trade.class, trade.getId(), LockModeType.PESSIMISTIC_WRITE)).willReturn(trade);

		// when & then
		assertThatThrownBy(() -> tradeRequestService.acceptTradeRequest(tradeRequestId, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.UNAUTHORIZED_TRADE_REQUEST_ACCESS);
	}

	@Test
	@DisplayName("교환 신청 수락 실패 - PENDING 상태가 아님")
	void acceptTradeRequest_fail_notPending() {
		// given
		Long tradeRequestId = 1L;
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

		TradeRequest tradeRequest = TradeRequest.builder()
			.trade(trade)
			.requesterId(200L)
			.requesterTicketId(10L)
			.build();
		tradeRequest.accept();

		given(entityManager.find(TradeRequest.class, tradeRequestId, LockModeType.PESSIMISTIC_WRITE)).willReturn(tradeRequest);
		given(entityManager.find(Trade.class, trade.getId(), LockModeType.PESSIMISTIC_WRITE)).willReturn(trade);

		// when & then
		assertThatThrownBy(() -> tradeRequestService.acceptTradeRequest(tradeRequestId, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("교환 신청 수락 실패 - Trade가 ACTIVE 상태가 아님")
	void acceptTradeRequest_fail_tradeNotActive() {
		// given
		Long tradeRequestId = 1L;
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
		trade.complete();

		TradeRequest tradeRequest = TradeRequest.builder()
			.trade(trade)
			.requesterId(200L)
			.requesterTicketId(10L)
			.build();

		given(entityManager.find(TradeRequest.class, tradeRequestId, LockModeType.PESSIMISTIC_WRITE)).willReturn(tradeRequest);
		given(entityManager.find(Trade.class, trade.getId(), LockModeType.PESSIMISTIC_WRITE)).willReturn(trade);

		// when & then
		assertThatThrownBy(() -> tradeRequestService.acceptTradeRequest(tradeRequestId, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("교환 신청 수락 실패 - 이미 수락된 신청이 있음")
	void acceptTradeRequest_fail_alreadyAccepted() {
		// given
		Long tradeRequestId = 1L;
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

		TradeRequest tradeRequest = TradeRequest.builder()
			.trade(trade)
			.requesterId(200L)
			.requesterTicketId(10L)
			.build();

		TradeRequest acceptedRequest = TradeRequest.builder()
			.trade(trade)
			.requesterId(201L)
			.requesterTicketId(11L)
			.build();
		acceptedRequest.accept();

		given(entityManager.find(TradeRequest.class, tradeRequestId, LockModeType.PESSIMISTIC_WRITE)).willReturn(tradeRequest);
		given(entityManager.find(Trade.class, trade.getId(), LockModeType.PESSIMISTIC_WRITE)).willReturn(trade);
		given(tradeRequestRepository.findByTradeAndStatus(trade, TradeRequestStatus.ACCEPTED))
			.willReturn(List.of(acceptedRequest));

		// when & then
		assertThatThrownBy(() -> tradeRequestService.acceptTradeRequest(tradeRequestId, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("교환 신청 거절 성공")
	void rejectTradeRequest_success() {
		// given
		Long tradeRequestId = 1L;
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

		TradeRequest tradeRequest = TradeRequest.builder()
			.trade(trade)
			.requesterId(200L)
			.requesterTicketId(10L)
			.build();

		given(entityManager.find(TradeRequest.class, tradeRequestId, LockModeType.PESSIMISTIC_WRITE)).willReturn(tradeRequest);
		given(entityManager.find(Trade.class, trade.getId(), LockModeType.PESSIMISTIC_WRITE)).willReturn(trade);

		// when
		tradeRequestService.rejectTradeRequest(tradeRequestId, memberId);

		// then
		assertThat(tradeRequest.getStatus()).isEqualTo(TradeRequestStatus.REJECTED);
	}

	@Test
	@DisplayName("교환 신청 거절 실패 - 권한 없음")
	void rejectTradeRequest_fail_unauthorized() {
		// given
		Long tradeRequestId = 1L;
		Long memberId = 999L;

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

		TradeRequest tradeRequest = TradeRequest.builder()
			.trade(trade)
			.requesterId(200L)
			.requesterTicketId(10L)
			.build();

		given(entityManager.find(TradeRequest.class, tradeRequestId, LockModeType.PESSIMISTIC_WRITE)).willReturn(tradeRequest);
		given(entityManager.find(Trade.class, trade.getId(), LockModeType.PESSIMISTIC_WRITE)).willReturn(trade);

		// when & then
		assertThatThrownBy(() -> tradeRequestService.rejectTradeRequest(tradeRequestId, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.UNAUTHORIZED_TRADE_REQUEST_ACCESS);
	}

	@Test
	@DisplayName("교환 신청 거절 실패 - PENDING 상태가 아님")
	void rejectTradeRequest_fail_notPending() {
		// given
		Long tradeRequestId = 1L;
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

		TradeRequest tradeRequest = TradeRequest.builder()
			.trade(trade)
			.requesterId(200L)
			.requesterTicketId(10L)
			.build();
		tradeRequest.reject();

		given(entityManager.find(TradeRequest.class, tradeRequestId, LockModeType.PESSIMISTIC_WRITE)).willReturn(tradeRequest);
		given(entityManager.find(Trade.class, trade.getId(), LockModeType.PESSIMISTIC_WRITE)).willReturn(trade);

		// when & then
		assertThatThrownBy(() -> tradeRequestService.rejectTradeRequest(tradeRequestId, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("양도(TRANSFER) 신청 수락 실패 - TRANSFER는 즉시 결제 방식")
	void acceptTransferTradeRequest_fail() {
		// given
		Long tradeRequestId = 1L;
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

		TradeRequest tradeRequest = TradeRequest.builder()
			.trade(trade)
			.requesterId(200L)
			.requesterTicketId(10L)
			.build();

		given(entityManager.find(TradeRequest.class, tradeRequestId, LockModeType.PESSIMISTIC_WRITE)).willReturn(tradeRequest);
		given(entityManager.find(Trade.class, trade.getId(), LockModeType.PESSIMISTIC_WRITE)).willReturn(trade);
		given(tradeRequestRepository.findByTradeAndStatus(trade, TradeRequestStatus.ACCEPTED))
			.willReturn(Collections.emptyList());

		// when & then
		assertThatThrownBy(() -> tradeRequestService.acceptTradeRequest(tradeRequestId, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("교환 신청 수락 실패 - 티켓이 ISSUED 상태가 아님")
	void acceptTradeRequest_fail_ticketNotIssued() throws Exception {
		// given
		Long tradeRequestId = 1L;
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

		TradeRequest tradeRequest = TradeRequest.builder()
			.trade(trade)
			.requesterId(200L)
			.requesterTicketId(10L)
			.build();

		Ticket ownerTicket = Ticket.builder()
			.reservationId(1L)
			.memberId(memberId)
			.seatId(1L)
			.build();

		Ticket requesterTicket = Ticket.builder()
			.reservationId(2L)
			.memberId(200L)
			.seatId(10L)
			.build();

		// Use reflection to set status to TRANSFERRED
		Field statusField = Ticket.class.getDeclaredField("status");
		statusField.setAccessible(true);
		statusField.set(ownerTicket, TicketStatus.TRANSFERRED);

		given(entityManager.find(TradeRequest.class, tradeRequestId, LockModeType.PESSIMISTIC_WRITE)).willReturn(tradeRequest);
		given(entityManager.find(Trade.class, trade.getId(), LockModeType.PESSIMISTIC_WRITE)).willReturn(trade);
		given(tradeRequestRepository.findByTradeAndStatus(trade, TradeRequestStatus.ACCEPTED))
			.willReturn(Collections.emptyList());
		given(ticketService.getTicketById(1L)).willReturn(ownerTicket);
		given(ticketService.getTicketById(10L)).willReturn(requesterTicket);

		// when & then
		assertThatThrownBy(() -> tradeRequestService.acceptTradeRequest(tradeRequestId, memberId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", TicketErrorCode.TICKET_NOT_TRANSFERABLE);
	}
}
