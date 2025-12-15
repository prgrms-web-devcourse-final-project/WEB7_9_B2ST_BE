package com.back.b2st.domain.trade.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateTradeReq {

	@NotNull(message = "가격은 필수입니다.")
	@Min(value = 1, message = "가격은 1원 이상이어야 합니다.")
	private Integer price;

	public UpdateTradeReq(Integer price) {
		this.price = price;
	}
}
