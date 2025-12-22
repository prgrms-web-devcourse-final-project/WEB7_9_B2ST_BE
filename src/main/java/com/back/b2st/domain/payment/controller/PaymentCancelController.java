package com.back.b2st.domain.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.payment.dto.request.PaymentCancelReq;
import com.back.b2st.domain.payment.dto.response.PaymentCancelRes;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.service.PaymentCancelService;
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
public class PaymentCancelController {

	private final PaymentCancelService paymentCancelService;

	@Operation(
		summary = "결제 취소",
		description = "완료된 결제를 취소합니다.\n\n"
			+ "- 현재는 TRADE(티켓 거래) 도메인만 취소 후처리를 지원합니다."
	)
	@PostMapping("/{orderId}/cancel")
	public ResponseEntity<BaseResponse<PaymentCancelRes>> cancel(
		@Parameter(description = "주문 ID", example = "ORDER-123") @PathVariable("orderId") String orderId,
		@Valid @RequestBody PaymentCancelReq request,
		@Parameter(hidden = true) @CurrentUser UserPrincipal user
	) {
		Payment canceledPayment = paymentCancelService.cancel(user.getId(), orderId, request);
		return ResponseEntity.ok(BaseResponse.success(PaymentCancelRes.from(canceledPayment)));
	}
}
