package com.back.b2st.domain.trade.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.trade.dto.request.CreateTradeRequest;
import com.back.b2st.domain.trade.dto.response.CreateTradeResponse;
import com.back.b2st.domain.trade.service.TradeService;
import com.back.b2st.global.common.BaseResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

	private final TradeService tradeService;

	@PostMapping
	public ResponseEntity<BaseResponse<CreateTradeResponse>> createTrade(
		@Valid @RequestBody CreateTradeRequest request
	) {
		// TODO: Spring Security에서 memberId 가져오기
		Long memberId = 1L;  // Mock

		CreateTradeResponse response = tradeService.createTrade(request, memberId);

		return ResponseEntity.ok(BaseResponse.success(response));
	}
}
