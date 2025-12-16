package com.back.b2st.domain.trade.dto.request;

import java.util.List;

import com.back.b2st.domain.trade.entity.TradeType;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateTradeReq {

	@NotEmpty(message = "티켓 ID 목록은 필수입니다.")
	private List<Long> ticketIds;

	@NotNull(message = "거래 유형은 필수입니다.")
	private TradeType type;

	private Integer price;  // 양도인 경우 필수

	// 테스트용 생성자
	public CreateTradeReq(List<Long> ticketIds, TradeType type, Integer price) {
		this.ticketIds = ticketIds;
		this.type = type;
		this.price = price;
	}
}
