package com.back.b2st.domain.trade.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.trade.dto.request.CreateTradeRequest;
import com.back.b2st.domain.trade.dto.request.UpdateTradeRequest;
import com.back.b2st.domain.trade.dto.response.CreateTradeResponse;
import com.back.b2st.domain.trade.dto.response.TradeResponse;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.entity.TradeType;
import com.back.b2st.domain.trade.service.TradeService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

	private final TradeService tradeService;

	@GetMapping
	public ResponseEntity<BaseResponse<Page<TradeResponse>>> getTrades(
		@RequestParam(required = false) TradeType type,
		@RequestParam(required = false) TradeStatus status,
		@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		Page<TradeResponse> response = tradeService.getTrades(type, status, pageable);
		return ResponseEntity.ok(BaseResponse.success(response));
	}

	@GetMapping("/{tradeId}")
	public ResponseEntity<BaseResponse<TradeResponse>> getTrade(@PathVariable Long tradeId) {
		TradeResponse response = tradeService.getTrade(tradeId);
		return ResponseEntity.ok(BaseResponse.success(response));
	}

	@PostMapping
	public ResponseEntity<BaseResponse<CreateTradeResponse>> createTrade(
		@Valid @RequestBody CreateTradeRequest request,
		@CurrentUser UserPrincipal userPrincipal
	) {
		Long memberId = (userPrincipal != null) ? userPrincipal.getId() : 1L;
		CreateTradeResponse response = tradeService.createTrade(request, memberId);

		return ResponseEntity.ok(BaseResponse.success(response));
	}

	@PatchMapping("/{tradeId}")
	public ResponseEntity<BaseResponse<Void>> updateTrade(
		@PathVariable Long tradeId,
		@Valid @RequestBody UpdateTradeRequest request,
		@CurrentUser UserPrincipal userPrincipal
	) {
		Long memberId = (userPrincipal != null) ? userPrincipal.getId() : 1L;
		tradeService.updateTrade(tradeId, request, memberId);

		return ResponseEntity.ok(BaseResponse.success(null));
	}

	@DeleteMapping("/{tradeId}")
	public ResponseEntity<BaseResponse<Void>> deleteTrade(
		@PathVariable Long tradeId,
		@CurrentUser UserPrincipal userPrincipal
	) {
		Long memberId = (userPrincipal != null) ? userPrincipal.getId() : 1L;
		tradeService.deleteTrade(tradeId, memberId);

		return ResponseEntity.ok(BaseResponse.success(null));
	}
}
