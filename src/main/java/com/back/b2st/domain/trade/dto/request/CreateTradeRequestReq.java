package com.back.b2st.domain.trade.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateTradeRequestReq(
	@NotNull(message = "교환할 티켓 ID는 필수입니다.")
	Long requesterTicketId
) {
}
