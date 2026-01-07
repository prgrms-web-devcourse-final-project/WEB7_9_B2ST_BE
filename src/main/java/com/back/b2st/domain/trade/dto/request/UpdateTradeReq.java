package com.back.b2st.domain.trade.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateTradeReq(
	@NotNull(message = "가격은 필수입니다.")
	@Min(value = 1, message = "가격은 1원 이상이어야 합니다.")
	Integer price
) {
}
