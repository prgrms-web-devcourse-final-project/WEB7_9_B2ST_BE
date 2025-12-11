package com.back.b2st.domain.trade.dto.request;

import com.back.b2st.domain.trade.entity.TradeType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateTradeRequest {

	@NotNull(message = "티켓 ID는 필수입니다.")
	private Long ticketId;

	@NotNull(message = "거래 유형은 필수입니다.")
	private TradeType type;

	private Integer price;  // 양도인 경우 필수

	@NotNull(message = "수량은 필수입니다.")
	@Min(value = 1, message = "수량은 1 이상이어야 합니다.")
	private Integer totalCount;

	// 테스트용 생성자
	public CreateTradeRequest(Long ticketId, TradeType type, Integer price, Integer totalCount) {
		this.ticketId = ticketId;
		this.type = type;
		this.price = price;
		this.totalCount = totalCount;
	}
}
