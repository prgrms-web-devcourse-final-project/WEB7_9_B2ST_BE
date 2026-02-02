package com.back.b2st.domain.payment.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

	import com.back.b2st.domain.payment.entity.DomainType;
	import com.back.b2st.domain.payment.error.PaymentErrorCode;
	import com.back.b2st.domain.seat.grade.entity.SeatGrade;
	import com.back.b2st.domain.seat.grade.entity.SeatGradeType;
	import com.back.b2st.domain.seat.grade.repository.SeatGradeRepository;
	import com.back.b2st.domain.ticket.entity.Ticket;
	import com.back.b2st.domain.ticket.error.TicketErrorCode;
	import com.back.b2st.domain.ticket.repository.TicketRepository;
	import com.back.b2st.domain.trade.entity.Trade;
	import com.back.b2st.domain.trade.entity.TradeType;
import com.back.b2st.domain.trade.error.TradeErrorCode;
import com.back.b2st.domain.trade.repository.TradeRepository;
import com.back.b2st.global.error.exception.BusinessException;

@SpringBootTest
@ActiveProfiles("test")
class TradePaymentHandlerTest {

	@Autowired
	private TradePaymentHandler tradePaymentHandler;

	@Autowired
	private TradeRepository tradeRepository;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private SeatGradeRepository seatGradeRepository;

	@BeforeEach
	void setup() {
		ticketRepository.deleteAll();
		tradeRepository.deleteAll();
		seatGradeRepository.deleteAll();
	}

	@Test
	@DisplayName("supports - TRADE 도메인 지원")
	void supports_trade() {
		assertThat(tradePaymentHandler.supports(DomainType.TRADE)).isTrue();
	}

	@Test
	@DisplayName("supports - 다른 도메인 미지원")
	void supports_other() {
		assertThat(tradePaymentHandler.supports(DomainType.RESERVATION)).isFalse();
		assertThat(tradePaymentHandler.supports(DomainType.LOTTERY)).isFalse();
	}

	@Test
	@DisplayName("loadAndValidate - Trade가 없으면 예외 발생")
	void loadAndValidate_tradeNotFound() {
		// when & then
		assertThatThrownBy(() -> tradePaymentHandler.loadAndValidate(999L, 1L))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException) ex).getErrorCode())
			.isEqualTo(TradeErrorCode.TRADE_NOT_FOUND);
	}

	@Test
	@DisplayName("loadAndValidate - EXCHANGE 타입이면 예외 발생")
	void loadAndValidate_exchangeType() {
		// given
		Long sellerId = 1L;
		Ticket ticket = Ticket.builder()
			.reservationId(1L)
			.memberId(sellerId)
			.seatId(1L)
			.qrCode("QR-1")
			.build();
		ticketRepository.save(ticket);

		Trade trade = Trade.builder()
			.memberId(sellerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(ticket.getId())
			.type(TradeType.EXCHANGE)
			.price(null)
			.totalCount(1)
			.section("A")
			.row("5")
			.seatNumber("12")
			.build();
		Trade savedTrade = tradeRepository.save(trade);

		// when & then
		assertThatThrownBy(() -> tradePaymentHandler.loadAndValidate(savedTrade.getId(), 2L))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("양도(TRANSFER) 타입만 결제가 필요합니다");
	}

	@Test
	@DisplayName("loadAndValidate - 본인 글이면 예외 발생")
	void loadAndValidate_ownTrade() {
		// given
		Long sellerId = 1L;
		Ticket ticket = Ticket.builder()
			.reservationId(1L)
			.memberId(sellerId)
			.seatId(1L)
			.qrCode("QR-2")
			.build();
		ticketRepository.save(ticket);

		Trade trade = Trade.builder()
			.memberId(sellerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(ticket.getId())
			.type(TradeType.TRANSFER)
			.price(50000)
			.totalCount(1)
			.section("A")
			.row("5")
			.seatNumber("12")
			.build();
		Trade savedTrade = tradeRepository.save(trade);

		// when & then
		assertThatThrownBy(() -> tradePaymentHandler.loadAndValidate(savedTrade.getId(), sellerId))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("본인의 양도글은 구매할 수 없습니다");
	}

	@Test
	@DisplayName("loadAndValidate - ACTIVE 상태가 아니면 예외 발생")
	void loadAndValidate_notActiveStatus() {
		// given
		Long sellerId = 1L;
		Ticket ticket = Ticket.builder()
			.reservationId(1L)
			.memberId(sellerId)
			.seatId(1L)
			.qrCode("QR-3")
			.build();
		ticketRepository.save(ticket);

		Trade trade = Trade.builder()
			.memberId(sellerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(ticket.getId())
			.type(TradeType.TRANSFER)
			.price(50000)
			.totalCount(1)
			.section("A")
			.row("5")
			.seatNumber("12")
			.build();
		trade.complete(); // COMPLETED 상태로 변경
		Trade savedTrade = tradeRepository.save(trade);

		// when & then
		assertThatThrownBy(() -> tradePaymentHandler.loadAndValidate(savedTrade.getId(), 2L))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("유효하지 않은 거래 상태입니다");
	}

	@Test
	@DisplayName("loadAndValidate - 가격이 null이면 예외 발생")
	void loadAndValidate_priceNull() {
		// given
		Long sellerId = 1L;
		Ticket ticket = Ticket.builder()
			.reservationId(1L)
			.memberId(sellerId)
			.seatId(1L)
			.qrCode("QR-4")
			.build();
		ticketRepository.save(ticket);

		Trade trade = Trade.builder()
			.memberId(sellerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(ticket.getId())
			.type(TradeType.TRANSFER)
			.price(null) // 가격 없음
			.totalCount(1)
			.section("A")
			.row("5")
			.seatNumber("12")
			.build();
		Trade savedTrade = tradeRepository.save(trade);

		// when & then
		assertThatThrownBy(() -> tradePaymentHandler.loadAndValidate(savedTrade.getId(), 2L))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("거래 가격이 설정되지 않았습니다");
	}

	@Test
	@DisplayName("loadAndValidate - 가격이 0 이하면 예외 발생")
	void loadAndValidate_priceZeroOrNegative() {
		// given
		Long sellerId = 1L;
		Ticket ticket = Ticket.builder()
			.reservationId(1L)
			.memberId(sellerId)
			.seatId(1L)
			.qrCode("QR-5")
			.build();
		ticketRepository.save(ticket);

		Trade trade = Trade.builder()
			.memberId(sellerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(ticket.getId())
			.type(TradeType.TRANSFER)
			.price(0) // 가격 0
			.totalCount(1)
			.section("A")
			.row("5")
			.seatNumber("12")
			.build();
		Trade savedTrade = tradeRepository.save(trade);

		// when & then
		assertThatThrownBy(() -> tradePaymentHandler.loadAndValidate(savedTrade.getId(), 2L))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("거래 가격이 설정되지 않았습니다");
	}

	@Test
	@DisplayName("loadAndValidate - 티켓이 ISSUED가 아니면 예외 발생")
	void loadAndValidate_ticketNotIssued() {
		// given
		Long sellerId = 1L;
		Ticket ticket = Ticket.builder()
			.reservationId(1L)
			.memberId(sellerId)
			.seatId(1L)
			.qrCode("QR-6")
			.build();
		ticket.use(); // USED 상태로 변경
		ticketRepository.save(ticket);

		Trade trade = Trade.builder()
			.memberId(sellerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(ticket.getId())
			.type(TradeType.TRANSFER)
			.price(50000)
			.totalCount(1)
			.section("A")
			.row("5")
			.seatNumber("12")
			.build();
		Trade savedTrade = tradeRepository.save(trade);

		// when & then
		assertThatThrownBy(() -> tradePaymentHandler.loadAndValidate(savedTrade.getId(), 2L))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException) ex).getErrorCode())
			.isEqualTo(TicketErrorCode.TICKET_NOT_TRANSFERABLE);
	}

	@Test
	@DisplayName("loadAndValidate - 티켓 소유자가 거래 등록자와 다르면 예외 발생")
	void loadAndValidate_ticketOwnerMismatch() {
		// given
		Long sellerId = 1L;
		Long actualOwnerId = 999L;

		Ticket ticket = Ticket.builder()
			.reservationId(1L)
			.memberId(actualOwnerId) // 다른 사람 소유
			.seatId(1L)
			.qrCode("QR-7")
			.build();
		ticketRepository.save(ticket);

		Trade trade = Trade.builder()
			.memberId(sellerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(ticket.getId())
			.type(TradeType.TRANSFER)
			.price(50000)
			.totalCount(1)
			.section("A")
			.row("5")
			.seatNumber("12")
			.build();
		Trade savedTrade = tradeRepository.save(trade);

		// when & then
		assertThatThrownBy(() -> tradePaymentHandler.loadAndValidate(savedTrade.getId(), 2L))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("티켓 소유자가 일치하지 않습니다");
	}

	@Test
	@DisplayName("loadAndValidate - 정상 케이스")
	void loadAndValidate_success() {
		// given
		Long sellerId = 1L;
		Long buyerId = 2L;
		Integer price = 50000;

		Ticket ticket = Ticket.builder()
			.reservationId(1L)
			.memberId(sellerId)
			.seatId(1L)
			.qrCode("QR-8")
			.build();
		ticketRepository.save(ticket);

		Trade trade = Trade.builder()
			.memberId(sellerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(ticket.getId())
			.type(TradeType.TRANSFER)
			.price(price)
			.totalCount(1)
			.section("A")
			.row("5")
			.seatNumber("12")
			.build();
		Trade savedTrade = tradeRepository.save(trade);

		seatGradeRepository.save(
			SeatGrade.builder()
				.performanceId(1L)
				.seatId(1L)
				.grade(SeatGradeType.STANDARD)
				.price(50000)
				.build()
		);

		// when
		PaymentTarget result = tradePaymentHandler.loadAndValidate(savedTrade.getId(), buyerId);

		// then
		assertThat(result.domainType()).isEqualTo(DomainType.TRADE);
		assertThat(result.domainId()).isEqualTo(savedTrade.getId());
		assertThat(result.expectedAmount()).isEqualTo(price.longValue());
	}

	@Test
	@DisplayName("loadAndValidate - 양도 가격이 정가를 초과하면 예외 발생")
	void loadAndValidate_priceAboveOriginal() {
		// given
		Long sellerId = 1L;
		Long buyerId = 2L;

		Ticket ticket = Ticket.builder()
			.reservationId(1L)
			.memberId(sellerId)
			.seatId(1L)
			.qrCode("QR-9")
			.build();
		ticketRepository.save(ticket);

		Trade trade = Trade.builder()
			.memberId(sellerId)
			.performanceId(1L)
			.scheduleId(1L)
			.ticketId(ticket.getId())
			.type(TradeType.TRANSFER)
			.price(60000)
			.totalCount(1)
			.section("A")
			.row("5")
			.seatNumber("12")
			.build();
		Trade savedTrade = tradeRepository.save(trade);

		seatGradeRepository.save(
			SeatGrade.builder()
				.performanceId(1L)
				.seatId(1L)
				.grade(SeatGradeType.STANDARD)
				.price(50000)
				.build()
		);

		// when & then
		assertThatThrownBy(() -> tradePaymentHandler.loadAndValidate(savedTrade.getId(), buyerId))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException) ex).getErrorCode())
			.isEqualTo(PaymentErrorCode.DOMAIN_NOT_PAYABLE);
	}
}
