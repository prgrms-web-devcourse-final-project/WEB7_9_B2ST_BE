package com.back.b2st.domain.trade.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.trade.dto.request.CreateTradeRequestReq;
import com.back.b2st.domain.trade.dto.response.TradeRequestRes;
import com.back.b2st.domain.trade.service.TradeRequestService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "TradeRequest", description = "교환/양도 신청 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TradeRequestController {

	private final TradeRequestService tradeRequestService;

	@Operation(summary = "교환/양도 신청", description = "등록된 교환/양도에 신청합니다 (EXCHANGE 타입의 경우 본인의 티켓 ID 필요)")
	@PostMapping("/trades/{tradeId}/requests")
	public ResponseEntity<BaseResponse<TradeRequestRes>> createTradeRequest(
		@Parameter(description = "거래 ID", example = "1") @PathVariable("tradeId") Long tradeId,
		@Valid @RequestBody CreateTradeRequestReq request,
		@Parameter(hidden = true) @CurrentUser UserPrincipal userPrincipal
	) {
		TradeRequestRes response = tradeRequestService.createTradeRequest(
			tradeId,
			request,
			userPrincipal.getId()
		);
		return ResponseEntity.ok(BaseResponse.success(response));
	}

	@Operation(summary = "교환/양도 신청 상세 조회", description = "특정 교환/양도 신청의 상세 정보를 조회합니다")
	@GetMapping("/trade-requests/{tradeRequestId}")
	public ResponseEntity<BaseResponse<TradeRequestRes>> getTradeRequest(
		@Parameter(description = "신청 ID", example = "1") @PathVariable("tradeRequestId") Long tradeRequestId
	) {
		TradeRequestRes response = tradeRequestService.getTradeRequest(tradeRequestId);
		return ResponseEntity.ok(BaseResponse.success(response));
	}

	@Operation(summary = "교환/양도 신청 목록 조회", description = "특정 거래에 대한 신청 목록 또는 내가 신청한 목록을 조회합니다 (둘 중 하나는 필수)")
	@GetMapping("/trade-requests")
	public ResponseEntity<BaseResponse<List<TradeRequestRes>>> getTradeRequests(
		@Parameter(description = "거래 ID (해당 거래에 대한 신청 목록 조회)", example = "1") @RequestParam(value = "tradeId", required = false) Long tradeId,
		@Parameter(description = "신청자 ID (내가 신청한 목록 조회)", example = "1") @RequestParam(value = "requesterId", required = false) Long requesterId
	) {
		if (tradeId == null && requesterId == null) {
			throw new IllegalArgumentException("tradeId 또는 requesterId 중 하나는 필수입니다.");
		}

		List<TradeRequestRes> responses;
		if (tradeId != null) {
			responses = tradeRequestService.getTradeRequestsByTrade(tradeId);
		} else {
			responses = tradeRequestService.getTradeRequestsByRequester(requesterId);
		}

		return ResponseEntity.ok(BaseResponse.success(responses));
	}

	@Operation(
		summary = "교환/양도 신청 수락 (티켓 소유권 이전)",
		description = "교환/양도 신청을 수락하고 티켓 소유권을 이전합니다.\n\n"
			+
			"- TRANSFER: 신청자에게 티켓 양도\n"
			+
			"- EXCHANGE: 서로의 티켓을 교환"
	)
	@PatchMapping("/trade-requests/{tradeRequestId}/accept")
	public BaseResponse<Void> acceptTradeRequest(
		@Parameter(description = "신청 ID", example = "1") @PathVariable("tradeRequestId") Long tradeRequestId,
		@Parameter(hidden = true) @CurrentUser UserPrincipal userPrincipal
	) {
		tradeRequestService.acceptTradeRequest(tradeRequestId, userPrincipal.getId());
		return BaseResponse.success(null);
	}

	@Operation(summary = "교환/양도 신청 거절", description = "교환/양도 신청을 거절합니다")
	@PatchMapping("/trade-requests/{tradeRequestId}/reject")
	public BaseResponse<Void> rejectTradeRequest(
		@Parameter(description = "신청 ID", example = "1") @PathVariable("tradeRequestId") Long tradeRequestId,
		@Parameter(hidden = true) @CurrentUser UserPrincipal userPrincipal
	) {
		tradeRequestService.rejectTradeRequest(tradeRequestId, userPrincipal.getId());
		return BaseResponse.success(null);
	}
}
