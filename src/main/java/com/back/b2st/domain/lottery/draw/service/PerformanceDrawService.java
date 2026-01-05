package com.back.b2st.domain.lottery.draw.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.lottery.draw.dto.LotteryApplicantInfo;
import com.back.b2st.domain.lottery.draw.dto.WeightedApplicant;
import com.back.b2st.domain.lottery.draw.dto.WinnerInfo;
import com.back.b2st.domain.lottery.entry.repository.LotteryEntryRepository;
import com.back.b2st.domain.lottery.result.entity.LotteryResult;
import com.back.b2st.domain.lottery.result.repository.LotteryResultRepository;
import com.back.b2st.domain.performanceschedule.dto.DrawTargetPerformance;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.seat.grade.entity.SeatGradeType;
import com.back.b2st.domain.seat.grade.repository.SeatGradeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceDrawService {

	private final PerformanceScheduleRepository performanceScheduleRepository;
	private final LotteryEntryRepository lotteryEntryRepository;
	private final SeatGradeRepository seatGradeRepository;
	private final LotteryResultRepository lotteryResultRepository;

	/**
	 * 마감 공연 조회
	 * @return DrawTargetPerformance = 공연id, 회차id
	 */
	public List<DrawTargetPerformance> findBookingClosedPerformances() {
		LocalDateTime startDate = LocalDate.now().minusDays(1).atStartOfDay();
		LocalDateTime endDate = LocalDate.now().atStartOfDay();

		return performanceScheduleRepository.findByClosedBetweenAndNotDrawn(startDate, endDate);
	}

	/**
	 * 공연 추첨 진행
	 * @param performanceId    공연 id
	 * @param scheduleId    회차 id
	 */
	@Transactional
	public void drawForPerformance(Long performanceId, Long scheduleId) {
		// 응모자 목록 가져오기
		List<LotteryApplicantInfo> entryInfos = lotteryEntryRepository.findAppliedInfoByScheduleId(
			scheduleId);

		if (entryInfos.size() == 0) {
			log.info("공연: {}, 회차: {} - 전체 응모자 없음", performanceId, scheduleId);
			return;
		}

		// 등급별 좌석 수
		EnumMap<SeatGradeType, Long> seatCountByGrade = toSeatCountMap(performanceId);

		// 등급별 응모자 그룹핑
		EnumMap<SeatGradeType, List<LotteryApplicantInfo>> byGrade = entryInfos.stream()
			.collect(Collectors.groupingBy(
				LotteryApplicantInfo::grade,
				() -> new EnumMap<>(SeatGradeType.class),
				Collectors.toList()
			));

		List<Long> allWinnerIds = new ArrayList<>();

		// 각 등급별 추첨 진행
		for (SeatGradeType gradeType : SeatGradeType.values()) {
			allWinnerIds.addAll(
				drawByGrade(gradeType, byGrade, seatCountByGrade, scheduleId)
			);
		}

		// 추첨 완료 회차 & !WIN => LOSE 일괄 변경
		lotteryEntryRepository.updateStatusBySchedule(scheduleId, allWinnerIds);
		performanceScheduleRepository.updateStautsById(scheduleId);

		// 응모 id와 당첨자 id 추출
		List<WinnerInfo> winnerInfos = entryInfos.stream()
			.filter(info -> allWinnerIds.contains(info.id()))
			.map(info -> new WinnerInfo(info.id(), info.memberId()))
			.toList();

		saveLotteryResult(winnerInfos);
		// todo db 저장 실패시 모든 당첨자 정보가 날아감 -> 이거 대한 처리를 어떻게 할 것인가?
		// todo 1. 발표하기 전에 모르니까 뭍을 건지, 아니면 파일로 따로 기록할 건지
	}

	/**
	 * 당첨자 저장
	 * @param winnerInfos 응모id, 사용자 id
	 */
	private void saveLotteryResult(List<WinnerInfo> winnerInfos) {
		List<LotteryResult> results = new ArrayList<>();

		for (WinnerInfo info : winnerInfos) {
			results.add(
				LotteryResult.builder()
					.lotteryEntryId(info.id())
					.memberId(info.memberId())
					.build()
			);
		}
		// todo saveAll 갱신
		lotteryResultRepository.saveAll(results);
	}

	/**
	 * 등급별 추첨 진행
	 * @param grade    등급
	 * @param byGrade    등급별 응모자 리스트
	 * @param seatCountByGrade    등급별 좌석 수
	 * @param scheduleId    공연 회차 id
	 */
	private List<Long> drawByGrade(
		SeatGradeType grade,
		Map<SeatGradeType, List<LotteryApplicantInfo>> byGrade,
		EnumMap<SeatGradeType, Long> seatCountByGrade,
		Long scheduleId
	) {
		// 해당 등급 응모자 리스트
		List<LotteryApplicantInfo> applicantInfos = byGrade.getOrDefault(grade, List.of());
		// 해당 등급 좌석 수
		Long seatCounts = seatCountByGrade.getOrDefault(grade, 0L);

		log.debug("등급: {}, 좌석 수: {}, 응모자 수: {}", grade, seatCounts, applicantInfos.size());

		if (applicantInfos.isEmpty() || seatCounts == 0) {
			log.info("잔여석{} 부족 또는 응모자{} 없음", seatCounts, applicantInfos.size());
			return List.of();
		}

		// 추첨 진행 + 가중치
		log.debug("등급 {} - 추첨 진행, 좌석 : {}", grade, seatCounts);
		List<Long> winnerIds = drawWithWeight(applicantInfos, seatCounts);
		log.debug("등급 {} - 당첨자 {}명 선정 완료", grade, winnerIds.size());
		return winnerIds;
	}

	/**
	 * 가중치 기반 추첨 - 신청 수량이 적을 수록 높은 가중치 부여
	 * @param applicantInfos 응모자 리스트
	 * @param seatCounts 좌석 수
	 * @return 당첨자 id 리스트
	 */
	private List<Long> drawWithWeight(List<LotteryApplicantInfo> applicantInfos, Long seatCounts) {
		// todo 가중치를 for문 말고 그냥 셔플링 해서 뽑는 방식으로 ㄱㄱ?
		// todo 중간에 당첨을 진행하다가 날아가는 경우 어떻게 할것인가? -> 한 명 뽑을 때마다 별도로 저장을 해야하나?
		// 가중치 계산
		List<WeightedApplicant> weightedApplicants = applicantInfos.stream()
			.map(applicant -> new WeightedApplicant(
				applicant,
				12 / applicant.quantity())
			).toList();

		// 전체 가중치
		int totalWeight = weightedApplicants.stream()
			.mapToInt(WeightedApplicant::weight)
			.sum();

		if (totalWeight <= 0)
			return List.of();

		// 추첨 진행
		List<Long> winnerIds = new ArrayList<>();
		Set<Long> selectedIds = new HashSet<>();
		long remainingSeats = seatCounts;    // 남은 좌석 수

		// 남은 좌석 0 이상,
		while (remainingSeats > 0 && selectedIds.size() < applicantInfos.size()) {
			int ramdomDraw = ThreadLocalRandom.current().nextInt(totalWeight) + 1;    // 0 ~ (totalWeight - 1)
			int currentWeight = 0;

			WeightedApplicant selected = null;

			for (WeightedApplicant applicant : weightedApplicants) {
				// 기존에 당점된 응모자 생략
				if (selectedIds.contains(applicant.applicantInfo().id())) {
					continue;
				}

				currentWeight += applicant.weight();
				// 현재 가중치 위치 비교
				if (ramdomDraw <= currentWeight) {
					selected = applicant;
					break;
				}
			}

			if (selected == null) {
				break;
			}

			int requestedQuantity = selected.applicantInfo().quantity();

			// 신청 수량 확인 후 잔여석 비교
			if (requestedQuantity <= remainingSeats) {
				// 당첨자 등록
				winnerIds.add(selected.applicantInfo().id());
				remainingSeats -= requestedQuantity;
			}

			// 선택된 응모자 제외 (잔여석 제한 포함)
			selectedIds.add(selected.applicantInfo().id());
			totalWeight -= selected.weight();

			log.debug("당첨: entryId={}, quantity={}, 남은 좌석={}", selected.applicantInfo().id(), requestedQuantity,
				remainingSeats);
		}

		log.debug("추첨 완료 - 당첨자: {}명, 배정 좌석: {}, 남은 좌석: {}", winnerIds.size(), seatCounts - remainingSeats,
			remainingSeats);

		return winnerIds;
	}

	private EnumMap<SeatGradeType, Long> toSeatCountMap(Long performanceId) {
		EnumMap<SeatGradeType, Long> map = new EnumMap<>(SeatGradeType.class);

		// 등급-좌석수 조회
		seatGradeRepository.countSeatGradesByGrade(performanceId)
			.forEach(s -> map.put(s.garde(), s.count()));

		for (SeatGradeType g : SeatGradeType.values()) {
			map.putIfAbsent(g, 0L);
		}
		return map;
	}
}
