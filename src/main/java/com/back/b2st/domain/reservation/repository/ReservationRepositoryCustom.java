package com.back.b2st.domain.reservation.repository;

import java.util.List;

import com.back.b2st.domain.reservation.dto.response.ReservationDetailRes;
import com.back.b2st.domain.reservation.dto.response.ReservationRes;

public interface ReservationRepositoryCustom {

	List<ReservationRes> findMyReservations(Long memberId);

	ReservationDetailRes findReservationDetail(Long reservationId, Long memberId);
}
