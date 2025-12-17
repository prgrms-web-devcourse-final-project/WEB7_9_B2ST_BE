package com.back.b2st.domain.reservation.repository;

import java.util.List;

import com.back.b2st.domain.reservation.dto.response.ReservationDetailRes;

public interface ReservationRepositoryCustom {

	List<ReservationDetailRes> findMyReservationDetails(Long memberId);

	ReservationDetailRes findReservationDetail(Long reservationId, Long memberId);
}
