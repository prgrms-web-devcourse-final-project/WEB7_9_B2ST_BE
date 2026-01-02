package com.back.b2st.domain.payment.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.UUID;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.payment.dto.request.PaymentPayReq;
import com.back.b2st.domain.payment.entity.DomainType;
import com.back.b2st.domain.payment.entity.PaymentMethod;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.service.PaymentOneClickService;
import com.back.b2st.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private PaymentOneClickService paymentOneClickService;

	private Authentication memberAuth;
	private Long memberId;

	@BeforeEach
	void setup() {
		memberId = 1L;

		UserPrincipal member = UserPrincipal.builder()
			.id(memberId)
			.email("member@test.com")
			.role("ROLE_MEMBER")
			.build();
		memberAuth = new UsernamePasswordAuthenticationToken(member, null, null);
	}

	@Test
	@DisplayName("원클릭 결제(pay) 성공 - 결제 DONE 반환")
	void pay_success() throws Exception {
		Payment payment = Payment.builder()
			.orderId("ORDER-1")
			.memberId(memberId)
			.domainType(DomainType.RESERVATION)
			.domainId(10L)
			.amount(15000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete(LocalDateTime.now());

		org.mockito.Mockito.when(paymentOneClickService.pay(
			org.mockito.ArgumentMatchers.eq(memberId),
			org.mockito.ArgumentMatchers.any(PaymentPayReq.class)
		)).thenReturn(payment);

		String body = objectMapper.writeValueAsString(new Object() {
			public final String domainType = "RESERVATION";
			public final Long domainId = 10L;
			public final String paymentMethod = "CARD";
		});

		// when & then
		mockMvc.perform(post("/api/payments/pay")
				.with(authentication(memberAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("DONE"));
	}

	@Test
	@DisplayName("추첨 예매 원클릭 결제(pay) 매핑 - 결제 DONE 반환")
	void pay_lottery_success() throws Exception {
		UUID entryId = UUID.randomUUID();
		String entryIdStr = entryId.toString();

		Payment payment = Payment.builder()
			.orderId("ORDER-LOTTERY-1")
			.memberId(memberId)
			.domainType(DomainType.LOTTERY)
			.domainId(99L)
			.amount(30000L)
			.method(PaymentMethod.CARD)
			.expiresAt(null)
			.build();
		payment.complete(LocalDateTime.now());

		org.mockito.Mockito.when(paymentOneClickService.pay(
			org.mockito.ArgumentMatchers.eq(memberId),
			org.mockito.ArgumentMatchers.any(PaymentPayReq.class)
		)).thenReturn(payment);

		String body = objectMapper.writeValueAsString(new Object() {
			public final String domainType = "LOTTERY";
			public final String paymentMethod = "CARD";
			public final String entryId = entryIdStr;
		});

		mockMvc.perform(post("/api/payments/pay")
				.with(authentication(memberAuth))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("DONE"));
	}
}
