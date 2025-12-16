package com.back.b2st.domain.trade.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.back.b2st.domain.trade.dto.request.CreateTradeReq;
import com.back.b2st.domain.trade.dto.request.UpdateTradeReq;
import com.back.b2st.domain.trade.dto.response.CreateTradeRes;
import com.back.b2st.domain.trade.dto.response.TradeRes;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.entity.TradeType;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "🎫 Trade", description = "티켓 교환/양도 API")
public interface TradeApi {

	@Operation(summary = "교환/양도 목록 조회", description = "필터링 옵션으로 교환/양도 목록을 조회합니다")
	ResponseEntity<BaseResponse<Page<TradeRes>>> getTrades(
		@Parameter(description = "거래 타입 (TRANSFER: 양도, EXCHANGE: 교환)") @RequestParam(value = "type", required = false) TradeType type,
		@Parameter(description = "거래 상태 (ACTIVE: 진행중, COMPLETED: 완료, CANCELED: 취소)") @RequestParam(value = "status", required = false) TradeStatus status,
		@Parameter(hidden = true) Pageable pageable
	);

	@Operation(summary = "교환/양도 상세 조회", description = "특정 교환/양도 건의 상세 정보를 조회합니다")
	ResponseEntity<BaseResponse<TradeRes>> getTrade(
		@Parameter(description = "거래 ID") @PathVariable("tradeId") Long tradeId
	);

	@Operation(summary = "교환/양도 등록", description = "새로운 교환/양도를 등록합니다. 교환은 1개, 양도는 1개 이상 가능합니다.")
	ResponseEntity<BaseResponse<java.util.List<CreateTradeRes>>> createTrade(
		@Valid @RequestBody CreateTradeReq request,
		@Parameter(hidden = true) UserPrincipal userPrincipal
	);

	@Operation(summary = "교환/양도 수정", description = "등록한 교환/양도 정보를 수정합니다")
	BaseResponse<Void> updateTrade(
		@Parameter(description = "거래 ID") @PathVariable("tradeId") Long tradeId,
		@Valid @RequestBody UpdateTradeReq request,
		@Parameter(hidden = true) UserPrincipal userPrincipal
	);

	@Operation(summary = "교환/양도 삭제", description = "등록한 교환/양도를 삭제합니다")
	BaseResponse<Void> deleteTrade(
		@Parameter(description = "거래 ID") @PathVariable("tradeId") Long tradeId,
		@Parameter(hidden = true) UserPrincipal userPrincipal
	);
}
