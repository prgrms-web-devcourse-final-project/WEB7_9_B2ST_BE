package com.back.b2st.domain.lottery.entry.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.lottery.entry.dto.response.SeatLayoutRes;
import com.back.b2st.domain.lottery.entry.service.LotteryEntryService;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class LotteryEntryController {

	private final LotteryEntryService lotteryEntryService;

	// TODO : performaces나 예매 Controller로 이동 예정
	@GetMapping("/performances/{performanceId}/lottery/section")
	public BaseResponse<SeatLayoutRes> getSeatLayout(
		@PathVariable("performanceId") Long performanceId
	) {
		try {
			return BaseResponse.success(lotteryEntryService.getSeatLayout(performanceId));
		} catch (BusinessException e) {
			return BaseResponse.error(e.getErrorCode());
		}
	}
}
