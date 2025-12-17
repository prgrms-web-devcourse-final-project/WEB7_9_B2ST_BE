package com.back.b2st.domain.reservation.repository;

import static com.back.b2st.domain.performance.entity.QPerformance.*;
import static com.back.b2st.domain.performanceschedule.entity.QPerformanceSchedule.*;
import static com.back.b2st.domain.reservation.entity.QReservation.*;
import static com.back.b2st.domain.seat.seat.entity.QSeat.*;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.back.b2st.domain.reservation.dto.response.ReservationDetailRes;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ReservationRepositoryImpl implements ReservationRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public ReservationDetailRes findReservationDetail(Long reservationId, Long memberId) {
		return queryFactory
			.select(
				Projections.constructor(
					ReservationDetailRes.class,
					reservation.id,
					reservation.status.stringValue(),
					Projections.constructor(
						ReservationDetailRes.PerformanceInfo.class,
						performance.performanceId,
						performance.title,
						performance.category,
						performance.startDate,
						performanceSchedule.startAt
					),
					Projections.constructor(
						ReservationDetailRes.SeatInfo.class,
						seat.sectionId,
						seat.sectionName,
						seat.rowLabel,
						seat.seatNumber
					)
				)
			)
			.from(reservation)
			.join(performanceSchedule)
			.on(reservation.performanceId.eq(performanceSchedule.performanceScheduleId))
			.join(performance)
			.on(performanceSchedule.performance.eq(performance))
			.join(seat)
			.on(reservation.seatId.eq(seat.id))
			.where(
				reservation.id.eq(reservationId),
				reservation.memberId.eq(memberId)
			)
			.fetchOne();
	}

	@Override
	public List<ReservationDetailRes> findMyReservationDetails(Long memberId) {
		return queryFactory
			.select(
				Projections.constructor(
					ReservationDetailRes.class,
					reservation.id,
					reservation.status.stringValue(),
					Projections.constructor(
						ReservationDetailRes.PerformanceInfo.class,
						performance.performanceId,
						performance.title,
						performance.category,
						performance.startDate,
						performanceSchedule.startAt
					),
					Projections.constructor(
						ReservationDetailRes.SeatInfo.class,
						seat.id,
						seat.sectionId,
						seat.sectionName,
						seat.rowLabel,
						seat.seatNumber
					)
				)
			)
			.from(reservation)
			.join(performanceSchedule)
			.on(reservation.performanceId.eq(performanceSchedule.performanceScheduleId))
			.join(performance)
			.on(performanceSchedule.performance.eq(performance))
			.join(seat)
			.on(reservation.seatId.eq(seat.id))
			.where(reservation.memberId.eq(memberId))
			.orderBy(reservation.createdAt.desc())
			.fetch();
	}
}
