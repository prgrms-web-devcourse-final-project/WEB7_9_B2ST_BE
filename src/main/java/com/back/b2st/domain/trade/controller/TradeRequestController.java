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

import com.back.b2st.domain.trade.api.TradeRequestApi;
import com.back.b2st.domain.trade.dto.request.CreateTradeRequestRequest;
import com.back.b2st.domain.trade.dto.response.TradeRequestResponse;
import com.back.b2st.domain.trade.service.TradeRequestService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TradeRequestController implements TradeRequestApi {

	private final TradeRequestService tradeRequestService;

	@Override
	@PostMapping("/trades/{tradeId}/requests")
	public ResponseEntity<BaseResponse<TradeRequestResponse>> createTradeRequest(
		@PathVariable("tradeId") Long tradeId,
		@Valid @RequestBody CreateTradeRequestRequest request,
		@CurrentUser UserPrincipal userPrincipal
	) {
		TradeRequestResponse response = tradeRequestService.createTradeRequest(
			tradeId,
			request,
			userPrincipal.getId()
		);
		return ResponseEntity.ok(BaseResponse.success(response));
	}

	@Override
	@GetMapping("/trade-requests/{tradeRequestId}")
	public ResponseEntity<BaseResponse<TradeRequestResponse>> getTradeRequest(
		@PathVariable("tradeRequestId") Long tradeRequestId
	) {
		TradeRequestResponse response = tradeRequestService.getTradeRequest(tradeRequestId);
		return ResponseEntity.ok(BaseResponse.success(response));
	}

	@Override
	@GetMapping("/trade-requests")
	public ResponseEntity<BaseResponse<List<TradeRequestResponse>>> getTradeRequests(
		@RequestParam(value = "tradeId", required = false) Long tradeId,
		@RequestParam(value = "requesterId", required = false) Long requesterId
	) {
		if (tradeId == null && requesterId == null) {
			throw new IllegalArgumentException("tradeId 또는 requesterId 중 하나는 필수입니다.");
		}

		List<TradeRequestResponse> responses;
		if (tradeId != null) {
			responses = tradeRequestService.getTradeRequestsByTrade(tradeId);
		} else {
			responses = tradeRequestService.getTradeRequestsByRequester(requesterId);
		}

		return ResponseEntity.ok(BaseResponse.success(responses));
	}

	@Override
	@PatchMapping("/trade-requests/{tradeRequestId}/accept")
	public BaseResponse<Void> acceptTradeRequest(
		@PathVariable("tradeRequestId") Long tradeRequestId,
		@CurrentUser UserPrincipal userPrincipal
	) {
		tradeRequestService.acceptTradeRequest(tradeRequestId, userPrincipal.getId());
		return BaseResponse.success(null);
	}

	@Override
	@PatchMapping("/trade-requests/{tradeRequestId}/reject")
	public BaseResponse<Void> rejectTradeRequest(
		@PathVariable("tradeRequestId") Long tradeRequestId,
		@CurrentUser UserPrincipal userPrincipal
	) {
		tradeRequestService.rejectTradeRequest(tradeRequestId, userPrincipal.getId());
		return BaseResponse.success(null);
	}
}