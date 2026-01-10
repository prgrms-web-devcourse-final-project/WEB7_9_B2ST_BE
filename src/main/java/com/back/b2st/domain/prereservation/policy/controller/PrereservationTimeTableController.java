package com.back.b2st.domain.prereservation.policy.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.prereservation.policy.dto.request.PrereservationTimeTableUpsertListReq;
import com.back.b2st.domain.prereservation.policy.dto.response.PrereservationTimeTableRes;
import com.back.b2st.domain.prereservation.policy.service.PrereservationTimeTableService;
import com.back.b2st.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/prereservations/schedules/{scheduleId}/timetables")
@Tag(name = "신청 예매(관리자)", description = "신청 예매 회차별 구역 활성 시간대(타임테이블) 관리 API")
public class PrereservationTimeTableController {

	private final PrereservationTimeTableService prereservationTimeTableService;

	@GetMapping
	@Operation(
		summary = "회차 타임테이블 조회",
		description = "신청 예매 회차의 구역별 예매 가능 시간대(타임테이블)를 조회합니다."
	)
	public BaseResponse<List<PrereservationTimeTableRes>> getTimeTables(
		@Parameter(description = "공연 회차 ID", example = "1")
		@PathVariable Long scheduleId
	) {
		var timeTables = prereservationTimeTableService.getTimeTables(scheduleId).stream()
			.map(PrereservationTimeTableRes::from)
			.toList();
		return BaseResponse.success(timeTables);
	}

	@PutMapping
	@Operation(
		summary = "회차 타임테이블 일괄 등록/수정",
		description = """
			신청 예매 회차의 구역별 예매 가능 시간대(타임테이블)를 일괄 등록/수정합니다.
			- (scheduleId, sectionId) 기준으로 upsert 처리됩니다.
			"""
	)
	public BaseResponse<Void> upsert(
		@Parameter(description = "공연 회차 ID", example = "1")
		@PathVariable Long scheduleId,
		@Valid @RequestBody PrereservationTimeTableUpsertListReq request
	) {
		prereservationTimeTableService.upsert(scheduleId, request.items());
		return BaseResponse.created(null);
	}
}
