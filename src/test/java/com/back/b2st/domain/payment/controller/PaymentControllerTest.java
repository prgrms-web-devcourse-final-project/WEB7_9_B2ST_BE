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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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

	@Autowired
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
		// given: 실제 데이터 사용 (InitData에서 생성된 데이터)
		String body = objectMapper.writeValueAsString(new Object() {
			public final String domainType = "RESERVATION";
			public final Long domainId = 1L;  // InitData의 실제 Reservation ID
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
		// given: 실제 LotteryEntry UUID 사용 (InitData에서 생성된 데이터)
		// 실제 UUID는 InitData에 따라 다를 수 있으므로, 테스트가 실패하면 조정 필요
		String entryIdStr = "00000000-0000-0000-0000-000000000001";  // 임시 UUID

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
