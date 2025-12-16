package com.back.b2st.domain.lottery.entry.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.lottery.entry.dto.request.RegisterLotteryEntryReq;
import com.back.b2st.domain.lottery.entry.dto.response.LotteryEntryInfo;
import com.back.b2st.domain.lottery.entry.dto.response.SectionLayoutRes;
import com.back.b2st.domain.lottery.entry.service.LotteryEntryService;
import com.back.b2st.global.common.BaseResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class LotteryEntryController {

	private final LotteryEntryService lotteryEntryService;

	// TODO : performaces나 예매 Controller로 이동 예정
	// 구역별 좌석 조회
	@GetMapping("/api/performances/{performanceId}/lottery/section")
	public BaseResponse<List<SectionLayoutRes>> getSeatLayout(
		@PathVariable("performanceId") Long performanceId
	) {
		return BaseResponse.success(lotteryEntryService.getSeatLayout(performanceId));
	}

	// TODO : performaces나 예매 Controller로 이동 예정
	// 추첨 응모 생성
	@PostMapping("/api/performances/{performanceId}/lottery/entry")
	public BaseResponse<LotteryEntryInfo> registerLotteryEntry(
		@PathVariable("performanceId") Long performanceId,
		@Valid @RequestBody RegisterLotteryEntryReq request
	) {
		return BaseResponse.created(lotteryEntryService.createLotteryEntry(performanceId, request));
	}
}
