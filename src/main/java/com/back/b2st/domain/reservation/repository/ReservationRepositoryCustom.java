package com.back.b2st.domain.reservation.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.back.b2st.domain.reservation.dto.response.ReservationDetailRes;
import com.back.b2st.domain.reservation.dto.response.ReservationRes;

public interface ReservationRepositoryCustom {

	/** 예매 목록 조회 */
	List<ReservationRes> findMyReservations(Long memberId);

	/** 특정 예매 상세 조회 (본인 소유 검증 포함) */
	ReservationDetailRes findReservationDetail(Long reservationId, Long memberId);

	/** 특정 예매 상세 조회 (소유 검증 없음) */
	ReservationDetailRes findReservationDetail(Long reservationId);

	/** 해당 좌석에 대해 이미 완료된 예매(COMPLETED)가 존재하는지 확인 */
	boolean existsCompletedByScheduleSeat(
		Long scheduleId,
		Long scheduleSeatId
	);

	/** 해당 좌석에 대해 아직 유효한 PENDING 예매가 존재하는지 확인 */
	boolean existsActivePendingByScheduleSeat(
		Long scheduleId,
		Long scheduleSeatId,
		LocalDateTime now
	);
}
