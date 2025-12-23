package com.back.b2st.domain.payment.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.entity.PaymentMethod;
import com.back.b2st.domain.payment.repository.PaymentRepository;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.security.UserPrincipal;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private ScheduleSeatRepository scheduleSeatRepository;

	private Authentication memberAuth;
	private Long memberId;

	@BeforeEach
	void setup() {
		paymentRepository.deleteAll();
		reservationRepository.deleteAll();
		scheduleSeatRepository.deleteAll();
		memberId = 1L;

		UserPrincipal member = UserPrincipal.builder()
			.id(memberId)
			.email("member@test.com")
			.role("ROLE_MEMBER")
			.build();
		memberAuth = new UsernamePasswordAuthenticationToken(member, null, null);
	}

	@Test
	@DisplayName("결제 승인(confirm) 성공 - READY → DONE")
	void confirm_success() throws Exception {
		// given
		String orderId = "ORDER-123";
		Long amount = 15000L;
		Long scheduleId = 100L;
		Long seatId = 200L;

		// 예매 및 좌석 생성
		Reservation reservation = Reservation.builder()
			.memberId(memberId)
			.scheduleId(scheduleId)
			.seatId(seatId)
			.expiresAt(LocalDateTime.now().plusMinutes(5))
			.build();
		reservationRepository.save(reservation);

		ScheduleSeat scheduleSeat = ScheduleSeat.builder()
			.scheduleId(scheduleId)
			.seatId(seatId)
			.build();
		scheduleSeat.hold(LocalDateTime.now().plusMinutes(5));
		scheduleSeatRepository.save(scheduleSeat);

		Payment payment = Payment.builder()
			.orderId(orderId)
			.memberId(memberId)
			.domainType(DomainType.RESERVATION)
			.domainId(reservation.getId())
			.amount(amount)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		paymentRepository.save(payment);

		String body = """
			{
			  "orderId": "%s",
			  "amount": %d
			}
			""".formatted(orderId, amount);

		// when & then
		mockMvc.perform(post("/api/payments/confirm")
				.with(authentication(memberAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.orderId").value(orderId))
			.andExpect(jsonPath("$.data.amount").value(amount))
			.andExpect(jsonPath("$.data.status").value("DONE"));
	}

	@Test
	@DisplayName("결제 승인(confirm) 멱등 - 이미 DONE이면 외부 호출 없이 200")
	void confirm_idempotent_done() throws Exception {
		// given
		String orderId = "ORDER-456";
		Long amount = 20000L;
		Long scheduleId = 101L;
		Long seatId = 201L;

		// 이미 확정된 예매 및 좌석 생성
		Reservation reservation = Reservation.builder()
			.memberId(memberId)
			.scheduleId(scheduleId)
			.seatId(seatId)
			.expiresAt(LocalDateTime.now().plusMinutes(5))
			.build();
		reservation.complete(LocalDateTime.now());
		reservationRepository.save(reservation);

		ScheduleSeat scheduleSeat = ScheduleSeat.builder()
			.scheduleId(scheduleId)
			.seatId(seatId)
			.build();
		scheduleSeat.sold();
		scheduleSeatRepository.save(scheduleSeat);

		Payment payment = Payment.builder()
			.orderId(orderId)
			.memberId(memberId)
			.domainType(DomainType.RESERVATION)
			.domainId(reservation.getId())
			.amount(amount)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete(LocalDateTime.now());
		paymentRepository.save(payment);

		String body = """
			{
			  "orderId": "%s",
			  "amount": %d
			}
			""".formatted(orderId, amount);

		// when & then
		mockMvc.perform(post("/api/payments/confirm")
				.with(authentication(memberAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("DONE"));
	}

	// TODO: 결제 취소(cancel) 및 실패(fail) 엔드포인트 통합 테스트 추가 필요
	// 현재 path variable을 사용하는 엔드포인트에 대한 MockMvc 테스트에서 404 오류가 발생하여 추후 해결 필요
}
