package com.back.b2st.domain.payment.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.payment.dto.request.PaymentPrepareReq;
import com.back.b2st.domain.payment.dto.response.PaymentPrepareRes;
import com.back.b2st.domain.payment.service.PaymentPrepareService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentPrepareController {

	private final PaymentPrepareService paymentPrepareService;

	@PostMapping("/prepare")
	public BaseResponse<PaymentPrepareRes> prepare(
		@CurrentUser UserPrincipal user,
		@Valid @RequestBody PaymentPrepareReq request
	) {
		PaymentPrepareRes response = paymentPrepareService.prepare(user.getId(), request);
		return BaseResponse.created(response);
	}
}

