package com.back.b2st.domain.lottery.entry.dto.response;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.back.b2st.domain.seat.seat.dto.response.SeatInfoRes;

public record SectionLayoutRes(
	String sectionName,
	List<GradeInfo> grades
) {
	public record GradeInfo(
		String grade,
		List<String> rows
	) {
	}

	public static List<SectionLayoutRes> from(List<SeatInfoRes> seats) {
		Map<String, List<SeatInfoRes>> bySectionName = seats.stream()
			.collect(Collectors.groupingBy(SeatInfoRes::sectionName));

		return bySectionName.entrySet().stream()
			.map(entry -> {
				String sectionName = entry.getKey();
				List<SeatInfoRes> sectionSeats = entry.getValue();

				Map<String, List<SeatInfoRes>> byGrade = sectionSeats.stream()
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