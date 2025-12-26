package com.back.b2st.domain.trade.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.trade.dto.response.TransferTradeHistoryRes;
import com.back.b2st.domain.trade.service.TradeHistoryService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "TradeHistory", description = "양도 구매/판매 내역 API")
@RestController
@RequestMapping("/api/mypage/trades/transfers")
@RequiredArgsConstructor
public class TradeHistoryController {

	private final TradeHistoryService tradeHistoryService;

	@Operation(
		summary = "내 양도 구매 내역 조회",
		description = "내가 구매한 양도(TRANSFER) 완료 내역을 조회합니다. 상대방 개인정보는 제공하지 않습니다."
	)
	@GetMapping("/purchases")
	public BaseResponse<List<TransferTradeHistoryRes>> getMyTransferPurchases(
		@CurrentUser UserPrincipal userPrincipal
	) {
		return BaseResponse.success(tradeHistoryService.getMyTransferPurchases(userPrincipal.getId()));
	}

	@Operation(
		summary = "내 양도 판매 내역 조회",
		description = "내가 판매한 양도(TRANSFER) 완료 내역을 조회합니다. 상대방 개인정보는 제공하지 않습니다."
	)
	@GetMapping("/sales")
	public BaseResponse<List<TransferTradeHistoryRes>> getMyTransferSales(
		@CurrentUser UserPrincipal userPrincipal
	) {
		return BaseResponse.success(tradeHistoryService.getMyTransferSales(userPrincipal.getId()));
	}
}

