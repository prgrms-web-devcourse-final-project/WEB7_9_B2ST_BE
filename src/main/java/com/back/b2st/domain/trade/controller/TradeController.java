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

import com.back.b2st.domain.trade.dto.request.CreateTradeReq;
import com.back.b2st.domain.trade.dto.request.UpdateTradeReq;
import com.back.b2st.domain.trade.dto.response.CreateTradeRes;
import com.back.b2st.domain.trade.dto.response.TradeRes;
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
	public ResponseEntity<BaseResponse<Page<TradeRes>>> getTrades(
		@RequestParam(value = "type", required = false) TradeType type,
		@RequestParam(value = "status", required = false) TradeStatus status,
		@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		Page<TradeRes> response = tradeService.getTrades(type, status, pageable);
		return ResponseEntity.ok(BaseResponse.success(response));
	}

	@GetMapping("/{tradeId}")
	public ResponseEntity<BaseResponse<TradeRes>> getTrade(@PathVariable("tradeId") Long tradeId) {
		TradeRes response = tradeService.getTrade(tradeId);
		return ResponseEntity.ok(BaseResponse.success(response));
	}

	@PostMapping
	public ResponseEntity<BaseResponse<CreateTradeRes>> createTrade(
		@Valid @RequestBody CreateTradeReq request,
		@CurrentUser UserPrincipal userPrincipal
	) {
		CreateTradeRes response = tradeService.createTrade(request, userPrincipal.getId());

		return ResponseEntity.ok(BaseResponse.success(response));
	}

	@PatchMapping("/{tradeId}")
	public BaseResponse<Void> updateTrade(
		@PathVariable("tradeId") Long tradeId,
		@Valid @RequestBody UpdateTradeReq request,
		@CurrentUser UserPrincipal userPrincipal
	) {
		tradeService.updateTrade(tradeId, request, userPrincipal.getId());
		return BaseResponse.success(null);
	}

	@DeleteMapping("/{tradeId}")
	public BaseResponse<Void> deleteTrade(
		@PathVariable("tradeId") Long tradeId,
		@CurrentUser UserPrincipal userPrincipal
	) {
		tradeService.deleteTrade(tradeId, userPrincipal.getId());
		return BaseResponse.success(null);
	}
}
