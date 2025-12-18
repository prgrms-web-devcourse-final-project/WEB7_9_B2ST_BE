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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Trade", description = "티켓 교환/양도 API")
@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

	private final TradeService tradeService;

	@Operation(summary = "교환/양도 목록 조회", description = "필터링 옵션으로 교환/양도 목록을 조회합니다")
	@GetMapping
	public ResponseEntity<BaseResponse<Page<TradeRes>>> getTrades(
		@Parameter(description = "거래 타입 (TRANSFER: 양도, EXCHANGE: 교환)") @RequestParam(value = "type", required = false) TradeType type,
		@Parameter(description = "거래 상태 (ACTIVE: 진행중, COMPLETED: 완료, CANCELED: 취소)") @RequestParam(value = "status", required = false) TradeStatus status,
		@Parameter(hidden = true) @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		Page<TradeRes> response = tradeService.getTrades(type, status, pageable);
		return ResponseEntity.ok(BaseResponse.success(response));
	}

	@Operation(summary = "교환/양도 상세 조회", description = "특정 교환/양도 건의 상세 정보를 조회합니다")
	@GetMapping("/{tradeId}")
	public ResponseEntity<BaseResponse<TradeRes>> getTrade(@Parameter(description = "거래 ID", example = "1") @PathVariable("tradeId") Long tradeId) {
		TradeRes response = tradeService.getTrade(tradeId);
		return ResponseEntity.ok(BaseResponse.success(response));
	}

	@Operation(summary = "교환/양도 등록", description = "새로운 교환/양도를 등록합니다. 교환은 1개, 양도는 1개 이상 가능합니다.")
	@PostMapping
	public ResponseEntity<BaseResponse<java.util.List<CreateTradeRes>>> createTrade(
		@Valid @RequestBody CreateTradeReq request,
		@Parameter(hidden = true) @CurrentUser UserPrincipal userPrincipal
	) {
		java.util.List<CreateTradeRes> response = tradeService.createTrade(request, userPrincipal.getId());
		return ResponseEntity.ok(BaseResponse.success(response));
	}

	@Operation(summary = "교환/양도 수정", description = "등록한 교환/양도 정보를 수정합니다")
	@PatchMapping("/{tradeId}")
	public BaseResponse<Void> updateTrade(
		@Parameter(description = "거래 ID", example = "1") @PathVariable("tradeId") Long tradeId,
		@Valid @RequestBody UpdateTradeReq request,
		@Parameter(hidden = true) @CurrentUser UserPrincipal userPrincipal
	) {
		tradeService.updateTrade(tradeId, request, userPrincipal.getId());
		return BaseResponse.success(null);
	}

	@Operation(summary = "교환/양도 삭제", description = "등록한 교환/양도를 삭제합니다")
	@DeleteMapping("/{tradeId}")
	public BaseResponse<Void> deleteTrade(
		@Parameter(description = "거래 ID", example = "1") @PathVariable("tradeId") Long tradeId,
		@Parameter(hidden = true) @CurrentUser UserPrincipal userPrincipal
	) {
		tradeService.deleteTrade(tradeId, userPrincipal.getId());
		return BaseResponse.success(null);
	}
}
