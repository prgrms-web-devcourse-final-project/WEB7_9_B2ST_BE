package com.back.b2st.domain.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.payment.dto.request.PaymentFailReq;
import com.back.b2st.domain.payment.dto.response.PaymentFailRes;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.service.PaymentFailService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentFailController {

	private final PaymentFailService paymentFailService;

	@PostMapping("/{orderId}/fail")
	public ResponseEntity<BaseResponse<PaymentFailRes>> fail(
		@PathVariable("orderId") String orderId,
		@Valid @RequestBody PaymentFailReq request,
		@CurrentUser UserPrincipal user
	) {
		Payment payment = paymentFailService.fail(user.getId(), orderId, request.reason());
		return ResponseEntity.ok(BaseResponse.success(PaymentFailRes.from(payment)));
	}
}

