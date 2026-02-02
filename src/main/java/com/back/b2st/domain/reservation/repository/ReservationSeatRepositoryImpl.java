package com.back.b2st.domain.reservation.repository;

import static com.back.b2st.domain.reservation.entity.QReservationSeat.*;
import static com.back.b2st.domain.scheduleseat.entity.QScheduleSeat.*;
import static com.back.b2st.domain.seat.seat.entity.QSeat.*;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.back.b2st.domain.reservation.dto.response.ReservationSeatInfo;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ReservationSeatRepositoryImpl implements ReservationSeatRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<ReservationSeatInfo> findSeatInfos(Long reservationId) {
		return queryFactory
			.select(
				Projections.constructor(
					ReservationSeatInfo.class,
					seat.id,
					seat.sectionId,
					seat.sectionName,
					seat.rowLabel,
					seat.seatNumber
				)
			)
			.from(reservationSeat)
			.join(scheduleSeat)
			.on(reservationSeat.scheduleSeatId.eq(scheduleSeat.id))
			.join(seat)
			.on(scheduleSeat.seatId.eq(seat.id))
			.where(reservationSeat.reservationId.eq(reservationId))
			.fetch();
	}

}
