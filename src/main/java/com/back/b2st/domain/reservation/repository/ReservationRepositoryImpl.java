package com.back.b2st.domain.reservation.repository;

import static com.back.b2st.domain.performance.entity.QPerformance.*;
import static com.back.b2st.domain.performanceschedule.entity.QPerformanceSchedule.*;
import static com.back.b2st.domain.reservation.entity.QReservation.*;
import static com.back.b2st.domain.reservation.entity.QReservationSeat.*;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.back.b2st.domain.reservation.dto.response.ReservationDetailRes;
import com.back.b2st.domain.reservation.dto.response.ReservationRes;
import com.back.b2st.domain.reservation.entity.ReservationStatus;
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
	public ReservationDetailRes findReservationDetail(Long reservationId) {
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
			.where(reservation.id.eq(reservationId))
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
			.where(
				reservation.memberId.eq(memberId),
				reservation.status.in(
					ReservationStatus.COMPLETED,
					ReservationStatus.CANCELED
				)
			)
			.orderBy(reservation.createdAt.desc())
			.fetch();
	}

	@Override
	public boolean existsCompletedByScheduleSeat(
		Long scheduleId,
		Long scheduleSeatId
	) {
		Integer result = queryFactory
			.selectOne()
			.from(reservation)
			.join(reservationSeat)
			.on(reservationSeat.reservationId.eq(reservation.id))
			.where(
				reservation.scheduleId.eq(scheduleId),
				reservationSeat.scheduleSeatId.eq(scheduleSeatId),
				reservation.status.eq(ReservationStatus.COMPLETED)
			)
			.fetchFirst();

		return result != null;
	}

	@Override
	public boolean existsActivePendingByScheduleSeat(
		Long scheduleId,
		Long scheduleSeatId,
		LocalDateTime now
	) {
		Integer result = queryFactory
			.selectOne()
			.from(reservation)
			.join(reservationSeat)
			.on(reservationSeat.reservationId.eq(reservation.id))
			.where(
				reservation.scheduleId.eq(scheduleId),
				reservationSeat.scheduleSeatId.eq(scheduleSeatId),
				reservation.status.eq(ReservationStatus.PENDING),
				reservation.expiresAt.gt(now)
			)
			.fetchFirst();

		return result != null;
	}
}
