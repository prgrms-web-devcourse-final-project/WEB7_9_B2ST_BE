package com.back.b2st.domain.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.payment.dto.request.PaymentConfirmReq;
import com.back.b2st.domain.payment.dto.response.PaymentConfirmRes;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.service.PaymentConfirmService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentConfirmController {

	private final PaymentConfirmService paymentConfirmService;

	@PostMapping("/confirm")
	public ResponseEntity<BaseResponse<PaymentConfirmRes>> confirm(
		@CurrentUser UserPrincipal user,
		@Valid @RequestBody PaymentConfirmReq request
	) {
		Payment payment = paymentConfirmService.confirm(user.getId(), request);
		return ResponseEntity.ok(BaseResponse.success(PaymentConfirmRes.from(payment)));
	}
}
