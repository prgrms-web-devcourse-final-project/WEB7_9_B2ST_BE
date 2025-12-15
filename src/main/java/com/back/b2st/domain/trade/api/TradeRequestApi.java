package com.back.b2st.domain.trade.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.back.b2st.domain.trade.dto.request.CreateTradeRequestRequest;
import com.back.b2st.domain.trade.dto.response.TradeRequestResponse;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "🤝 TradeRequest", description = "교환/양도 신청 API")
public interface TradeRequestApi {

	@Operation(summary = "교환/양도 신청", description = "등록된 교환/양도에 신청합니다 (EXCHANGE 타입의 경우 본인의 티켓 ID 필요)")
	ResponseEntity<BaseResponse<TradeRequestResponse>> createTradeRequest(
		@Parameter(description = "거래 ID") @PathVariable("tradeId") Long tradeId,
		@Valid @RequestBody CreateTradeRequestRequest request,
		@Parameter(hidden = true) UserPrincipal userPrincipal
	);

	@Operation(summary = "교환/양도 신청 상세 조회", description = "특정 교환/양도 신청의 상세 정보를 조회합니다")
	ResponseEntity<BaseResponse<TradeRequestResponse>> getTradeRequest(
		@Parameter(description = "신청 ID") @PathVariable("tradeRequestId") Long tradeRequestId
	);

	@Operation(summary = "교환/양도 신청 목록 조회", description = "특정 거래에 대한 신청 목록 또는 내가 신청한 목록을 조회합니다 (둘 중 하나는 필수)")
	ResponseEntity<BaseResponse<List<TradeRequestResponse>>> getTradeRequests(
		@Parameter(description = "거래 ID (해당 거래에 대한 신청 목록 조회)") @RequestParam(value = "tradeId", required = false) Long tradeId,
		@Parameter(description = "신청자 ID (내가 신청한 목록 조회)") @RequestParam(value = "requesterId", required = false) Long requesterId
	);

	@Operation(
		summary = "교환/양도 신청 수락 (티켓 소유권 이전)",
		description = "교환/양도 신청을 수락하고 티켓 소유권을 이전합니다.\n\n" +
			"- TRANSFER: 신청자에게 티켓 양도\n" +
			"- EXCHANGE: 서로의 티켓을 교환"
	)
	BaseResponse<Void> acceptTradeRequest(
		@Parameter(description = "신청 ID") @PathVariable("tradeRequestId") Long tradeRequestId,
		@Parameter(hidden = true) UserPrincipal userPrincipal
	);

	@Operation(summary = "교환/양도 신청 거절", description = "교환/양도 신청을 거절합니다")
	BaseResponse<Void> rejectTradeRequest(
		@Parameter(description = "신청 ID") @PathVariable("tradeRequestId") Long tradeRequestId,
		@Parameter(hidden = true) UserPrincipal userPrincipal
	);
}
