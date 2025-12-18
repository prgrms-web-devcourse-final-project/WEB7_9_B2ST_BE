package com.back.b2st.domain.trade.dto.request;

import java.util.List;

import com.back.b2st.domain.trade.entity.TradeType;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateTradeReq(
	@NotEmpty(message = "티켓 ID 목록은 필수입니다.")
	List<Long> ticketIds,

	@NotNull(message = "거래 유형은 필수입니다.")
	TradeType type,

	Integer price
) {
}
