package com.back.b2st.domain.lottery.entry.dto.response;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.back.b2st.domain.seat.grade.entity.SeatGradeType;
import com.back.b2st.domain.seat.seat.dto.response.SeatInfoRes;

/**
 * 좌석 배치도 전달
 * @param sectionName    구역명
 * @param grades    등급에 해당하는 열 정보
 */
public record SectionLayoutRes(
	String sectionName,
	List<GradeInfo> grades
) {
	public record GradeInfo(
		SeatGradeType grade,
		List<String> rows
	) {
	}

	public static List<SectionLayoutRes> from(List<SeatInfoRes> seats) {
		// 구역으로 묶음
		Map<String, List<SeatInfoRes>> bySectionName = seats.stream()
			.collect(Collectors.groupingBy(SeatInfoRes::sectionName));

		return bySectionName.entrySet().stream()
			.map(entry -> {
				String sectionName = entry.getKey();
				List<SeatInfoRes> sectionSeats = entry.getValue();

				// 등급으로 묶음
				Map<SeatGradeType, List<SeatInfoRes>> byGrade = sectionSeats.stream()
					.collect(Collectors.groupingBy(SeatInfoRes::grade));

				List<GradeInfo> grades = byGrade.entrySet().stream()
					.map(gradeEntry -> new GradeInfo(
						gradeEntry.getKey(),
						gradeEntry.getValue().stream()
							.map(SeatInfoRes::rowLabel)
							.distinct()
							.toList()
					))
					.toList();

			return new SectionLayoutRes(sectionName, grades);
		})
			.toList();
	}
}
