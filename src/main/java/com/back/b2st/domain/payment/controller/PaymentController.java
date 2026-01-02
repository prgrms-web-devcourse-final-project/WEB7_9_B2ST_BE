package com.back.b2st.domain.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.payment.dto.request.PaymentPayReq;
import com.back.b2st.domain.payment.dto.response.PaymentConfirmRes;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.service.PaymentOneClickService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Payment", description = "결제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

	private final PaymentOneClickService paymentOneClickService;

	@Operation(
		summary = "원클릭 결제 (PG 미사용)",
		description = "결제 준비/승인/도메인 후처리를 한 번에 수행합니다.\n\n"
			+ "- LOTTERY: entryId(UUID) 필수\n"
			+ "- 그 외: domainId(Long) 필수"
	)
	@PostMapping("/pay")
	public ResponseEntity<BaseResponse<PaymentConfirmRes>> pay(
		@Parameter(hidden = true) @CurrentUser UserPrincipal user,
		@Valid @RequestBody PaymentPayReq request
	) {
		Payment payment = paymentOneClickService.pay(user.getId(), request);
		return ResponseEntity.ok(BaseResponse.success(PaymentConfirmRes.from(payment)));
	}
}
