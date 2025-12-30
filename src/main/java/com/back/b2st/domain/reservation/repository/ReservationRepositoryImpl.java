package com.back.b2st.domain.reservation.repository;

import static com.back.b2st.domain.performance.entity.QPerformance.*;
import static com.back.b2st.domain.performanceschedule.entity.QPerformanceSchedule.*;
import static com.back.b2st.domain.reservation.entity.QReservation.*;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.back.b2st.domain.reservation.dto.response.ReservationDetailRes;
import com.back.b2st.domain.reservation.dto.response.ReservationRes;
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
						performanceSchedule.performanceScheduleId,
						performance.title,
						performance.category,
						performance.startDate,
						performanceSchedule.startAt
					)
				)
			)
			.from(reservation)
			.join(performanceSchedule)
			.on(reservation.scheduleId.eq(performanceSchedule.performanceScheduleId))
			.join(performance)
			.on(performanceSchedule.performance.eq(performance))
			.where(
				reservation.id.eq(reservationId),
				reservation.memberId.eq(memberId)
			)
			.fetchOne();
	}

	@Override
	public List<ReservationRes> findMyReservations(Long memberId) {
		return queryFactory
			.select(
				Projections.constructor(
					ReservationRes.class,
					reservation.id,
					reservation.status.stringValue(),
					Projections.constructor(
						ReservationRes.PerformanceInfo.class,
						performance.performanceId,
						performanceSchedule.performanceScheduleId,
						performance.title,
						performance.category,
						performance.startDate,
						performanceSchedule.startAt
					)
				)
			)
			.from(reservation)
			.join(performanceSchedule)
			.on(reservation.scheduleId.eq(performanceSchedule.performanceScheduleId))
			.join(performance)
			.on(performanceSchedule.performance.eq(performance))
			.where(reservation.memberId.eq(memberId))
			.orderBy(reservation.createdAt.desc())
			.fetch();
	}
}
