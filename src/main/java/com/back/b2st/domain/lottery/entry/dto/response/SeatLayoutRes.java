package com.back.b2st.domain.lottery.entry.dto.response;

public record SeatLayoutRes(
	// TODO: 이거 id 이름 수정
	Long seatId,    // 좌석 번호
	String sectionName,    // 구역
	String rowLabel,    // 열
	String seatNumber    // 좌석번호
) {
	public static SeatLayoutRes fromEntity(Long seatId, String section, String row, String seat) {
		return new SeatLayoutRes(
			seatId,
			section,
			row,
			seat
		);
	}

}
