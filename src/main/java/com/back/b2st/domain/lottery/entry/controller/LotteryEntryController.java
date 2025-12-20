package com.back.b2st.domain.lottery.entry.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.lottery.entry.dto.request.RegisterLotteryEntryReq;
import com.back.b2st.domain.lottery.entry.dto.response.AppliedLotteryInfo;
import com.back.b2st.domain.lottery.entry.dto.response.LotteryEntryInfo;
import com.back.b2st.domain.lottery.entry.dto.response.SectionLayoutRes;
import com.back.b2st.domain.lottery.entry.service.LotteryEntryService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Lottery", description = "추첨 예매 API")
public class LotteryEntryController {

	private final LotteryEntryService lotteryEntryService;

	@Operation(
		summary = "구역별 좌석 배치 조회",
		description = "공연 ID로 추첨 예매에 사용되는 구역별 좌석 배치 정보를 조회"
	)
	@GetMapping("/api/performances/{performanceId}/lottery/section")
	public BaseResponse<List<SectionLayoutRes>> getSeatLayout(
		@CurrentUser UserPrincipal userPrincipal,
		@PathVariable("performanceId") Long performanceId
	) {
		return BaseResponse.success(lotteryEntryService.getSeatLayout(userPrincipal.getId(), performanceId));
	}

	@Operation(
		summary = "추첨 응모 생성",
		description = "공연에 대해 추첨 예매 응모를 생성 - 로그인 사용자만 가능"
	)
	@PostMapping("/api/performances/{performanceId}/lottery/entry")
	public BaseResponse<LotteryEntryInfo> registerLotteryEntry(
		@CurrentUser UserPrincipal userPrincipal,
		@PathVariable("performanceId") Long performanceId,
		@Valid @RequestBody RegisterLotteryEntryReq request
	) {
		return BaseResponse.created(
			lotteryEntryService.createLotteryEntry(userPrincipal.getId(), performanceId, request));
	}

	@Operation(
		summary = "내 응모 내역 조회",
		description = "내가 응모한 공연 내역을 조회 - 내 것만 조회가능"
	)
	@GetMapping("/api/mypage/lottery/entries")
	public BaseResponse<List<AppliedLotteryInfo>> getMyLotteryEntry(
		@CurrentUser UserPrincipal userPrincipal
	) {
		return BaseResponse.success(lotteryEntryService.getMyLotteryEntry(userPrincipal.getId()));
	}
}
