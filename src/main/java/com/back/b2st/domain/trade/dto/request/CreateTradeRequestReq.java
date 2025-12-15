package com.back.b2st.domain.trade.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateTradeRequestReq {

	@NotNull(message = "교환할 티켓 ID는 필수입니다.")
	private Long requesterTicketId;

	public CreateTradeRequestReq(Long requesterTicketId) {
		this.requesterTicketId = requesterTicketId;
	}
}
