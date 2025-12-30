package com.back.b2st.domain.reservation.repository;

import java.util.List;

import com.back.b2st.domain.reservation.dto.response.ReservationSeatInfo;

public interface ReservationSeatRepositoryCustom {

	List<ReservationSeatInfo> findSeatInfos(Long reservationId);

}
