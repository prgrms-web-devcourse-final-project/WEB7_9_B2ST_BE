package com.back.b2st.domain.performanceschedule.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.performanceschedule.dto.request.PerformanceScheduleCreateReq;
import com.back.b2st.domain.performanceschedule.dto.response.PerformanceScheduleCreateRes;
import com.back.b2st.domain.performanceschedule.dto.response.PerformanceScheduleDetailRes;
import com.back.b2st.domain.performanceschedule.dto.response.PerformanceScheduleListRes;
import com.back.b2st.domain.performanceschedule.service.PerformanceScheduleService;
import com.back.b2st.global.common.BaseResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/performances")
public class PerformanceScheduleController {

	private final PerformanceScheduleService performanceScheduleService;

	/**
	 * 공연 회차 생성 (관리자)
	 * POST /api/performances/{performanceId}/schedules
	 */
	@PostMapping("/{performanceId}/schedules")
	public BaseResponse<PerformanceScheduleCreateRes> createSchedule(
			@PathVariable Long performanceId,
			@Valid @RequestBody PerformanceScheduleCreateReq request
	) {
		PerformanceScheduleCreateRes res =
				performanceScheduleService.createSchedule(performanceId, request);
		return BaseResponse.created(res);
	}

	/**
	 * 공연별 회차 목록 조회
	 * GET /api/performances/{performanceId}/schedules
	 */
	@GetMapping("/{performanceId}/schedules")
	public BaseResponse<List<PerformanceScheduleListRes>> getSchedules(
			@PathVariable Long performanceId
	) {
		List<PerformanceScheduleListRes> res =
				performanceScheduleService.getSchedules(performanceId);
		return BaseResponse.success(res);
	}

	/**
	 * 공연 회차 단건 조회
	 * GET /api/performances/{performanceId}/schedules/{scheduleId}
	 */
	@GetMapping("/{performanceId}/schedules/{scheduleId}")
	public BaseResponse<PerformanceScheduleDetailRes> getSchedule(
			@PathVariable Long performanceId,
			@PathVariable Long scheduleId
	) {
		PerformanceScheduleDetailRes res =
				performanceScheduleService.getSchedule(performanceId, scheduleId);
		return BaseResponse.success(res);
	}
}
