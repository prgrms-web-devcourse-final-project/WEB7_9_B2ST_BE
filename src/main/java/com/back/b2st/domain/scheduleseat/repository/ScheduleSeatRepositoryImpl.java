package com.back.b2st.domain.scheduleseat.repository;

import static com.back.b2st.domain.scheduleseat.entity.QScheduleSeat.*;
import static com.back.b2st.domain.seat.seat.entity.QSeat.*;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.back.b2st.domain.scheduleseat.dto.response.ScheduleSeatViewRes;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ScheduleSeatRepositoryImpl implements ScheduleSeatRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	/** 측정 회차 전체 좌석 조회 */
	@Override
	public List<ScheduleSeatViewRes> findSeats(Long scheduleId) {
		return queryFactory
			.select(
				Projections.constructor(
					ScheduleSeatViewRes.class,
					scheduleSeat.id,
					seat.id,
					seat.sectionName,
					seat.rowLabel,
					seat.seatNumber,
					scheduleSeat.status
				)
			)
			.from(scheduleSeat)
			.join(seat)
			.on(scheduleSeat.seatId.eq(seat.id))
			.where(
				scheduleSeat.scheduleId.eq(scheduleId)
			)
			.orderBy(
				seat.sectionName.asc(),
				seat.rowLabel.asc(),
				seat.seatNumber.asc()
			)
			.fetch();
	}

	/** 특정 회차 지정한 상태의 좌석만 조회 */
	@Override
	public List<ScheduleSeatViewRes> findSeatsByStatus(
		Long scheduleId,
		SeatStatus status
	) {
		return queryFactory
			.select(
				Projections.constructor(
					ScheduleSeatViewRes.class,
					scheduleSeat.id,
					seat.id,
					seat.sectionName,
					seat.rowLabel,
					seat.seatNumber,
					scheduleSeat.status
				)
			)
			.from(scheduleSeat)
			.join(seat)
			.on(scheduleSeat.seatId.eq(seat.id))
			.where(
				scheduleSeat.scheduleId.eq(scheduleId),
				scheduleSeat.status.eq(status)
			)
			.orderBy(
				seat.sectionName.asc(),
				seat.rowLabel.asc(),
				seat.seatNumber.asc()
			)
			.fetch();
	}
}
