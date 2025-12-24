package com.back.b2st.domain.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.payment.dto.request.PaymentCancelReq;
import com.back.b2st.domain.payment.dto.request.PaymentConfirmReq;
import com.back.b2st.domain.payment.dto.request.PaymentFailReq;
import com.back.b2st.domain.payment.dto.request.PaymentPrepareReq;
import com.back.b2st.domain.payment.dto.response.PaymentCancelRes;
import com.back.b2st.domain.payment.dto.response.PaymentConfirmRes;
import com.back.b2st.domain.payment.dto.response.PaymentFailRes;
import com.back.b2st.domain.payment.dto.response.PaymentPrepareRes;
import com.back.b2st.domain.payment.entity.Payment;
import com.back.b2st.domain.payment.service.PaymentCancelService;
import com.back.b2st.domain.payment.service.PaymentConfirmService;
import com.back.b2st.domain.payment.service.PaymentFailService;
import com.back.b2st.domain.payment.service.PaymentPrepareService;
import com.back.b2st.domain.reservation.service.ReservationService;
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

	private final PaymentPrepareService paymentPrepareService;
	private final PaymentConfirmService paymentConfirmService;
	private final PaymentCancelService paymentCancelService;
	private final PaymentFailService paymentFailService;

	private final ReservationService reservationService;

	@Operation(
		summary = "결제 준비",
		description = "결제 정보를 생성하고 orderId를 반환합니다."
	)
	@PostMapping("/prepare")
	public ResponseEntity<BaseResponse<PaymentPrepareRes>> prepare(
		@Parameter(hidden = true) @CurrentUser UserPrincipal user,
		@Valid @RequestBody PaymentPrepareReq request
	) {
		Payment payment = paymentPrepareService.prepare(user.getId(), request);
		return ResponseEntity.ok(BaseResponse.created(PaymentPrepareRes.from(payment)));
	}

	@Operation(
		summary = "결제 승인",
		description = "결제를 승인하고 도메인별 후처리를 수행합니다."
	)
	@PostMapping("/confirm")
	public ResponseEntity<BaseResponse<PaymentConfirmRes>> confirm(
		@Parameter(hidden = true) @CurrentUser UserPrincipal user,
		@Valid @RequestBody PaymentConfirmReq request
	) {
		Payment payment = paymentConfirmService.confirm(user.getId(), request);
		return ResponseEntity.ok(BaseResponse.success(PaymentConfirmRes.from(payment)));
	}

	@Operation(
		summary = "결제 취소",
		description = "완료된 결제를 취소합니다.\n\n"
			+ "- 티켓 거래(TRADE) 결제는 취소/환불을 지원하지 않습니다.\n"
			+ "- 예매(RESERVATION) 결제는 취소/환불을 지원하지 않습니다."
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

	@Operation(
		summary = "결제 실패 처리",
		description = "결제 실패 시 호출되며, 도메인별 실패 후처리를 수행합니다."
	)
	@PostMapping("/{orderId}/fail")
	public ResponseEntity<BaseResponse<PaymentFailRes>> fail(
		@Parameter(description = "주문 ID", example = "ORDER-123") @PathVariable("orderId") String orderId,
		@Valid @RequestBody PaymentFailReq request,
		@Parameter(hidden = true) @CurrentUser UserPrincipal user
	) {
		Payment payment = paymentFailService.fail(user.getId(), orderId, request.reason());
		return ResponseEntity.ok(BaseResponse.success(PaymentFailRes.from(payment)));
	}
}
