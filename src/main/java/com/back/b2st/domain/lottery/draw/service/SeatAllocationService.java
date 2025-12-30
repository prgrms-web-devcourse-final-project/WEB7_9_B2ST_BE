package com.back.b2st.domain.lottery.draw.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.lottery.result.dto.LotteryReservationInfo;
import com.back.b2st.domain.lottery.result.repository.LotteryResultRepository;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatAllocationService {

	private final ScheduleSeatRepository scheduleSeatRepository;
	private final ReservationRepository reservationRepository;
	private final LotteryResultRepository lotteryResultRepository;

	public List<LotteryReservationInfo> findReservationInfos() {
		return lotteryResultRepository.findReservationInfoByPaidIsTrue();
	}

	@Transactional
	public List<ScheduleSeat> allocateSeatsForLottery(LotteryReservationInfo info) {
		// AVAILABLE 좌석 조회
		List<ScheduleSeat> availableSeats = scheduleSeatRepository.findAvailableSeatsByGrade(
			info.scheduleId(),
			info.grade());

		// 좌석 부족 체크
		if (availableSeats.size() < info.quantity()) {
			throw new IllegalStateException(
				String.format("좌석 부족 - scheduleId: %d, grade: %s, 필요: %d, 가능: %d",
					info.scheduleId(), info.grade(), info.quantity(), availableSeats.size())
			);
		}

		Collections.shuffle(availableSeats);

		// 좌석 배정
		List<ScheduleSeat> allocatedSeats = availableSeats.stream()
			.limit(info.quantity())
			.toList();

		allocatedSeats.forEach(ScheduleSeat::sold);

		log.info("좌석 배정 완료 - resultId: {}, memberId: {}, 배정 좌석 수: {}",
			info.resultId(), info.memberId(), allocatedSeats.size());

		return allocatedSeats;

	}

	/**
	 * 예매 생성
	 */
	private List<Reservation> createReservation(
		LotteryReservationInfo info, List<ScheduleSeat> seats
	) {
		List<Reservation> reservations = seats.stream()
			.map(seat -> createReservation(info, seat))
			.toList();

		reservationRepository.saveAll(reservations);

		log.info("좌석 배정 완료 - resultId: {}, memberId: {}, 배정 좌석 수: {}",
			info.resultId(), info.memberId(), reservations.size());

		return reservations;
	}

	private Reservation createReservation(LotteryReservationInfo info, ScheduleSeat seat) {
		Reservation reservation = Reservation.builder()
			.scheduleId(info.scheduleId())
			.memberId(info.memberId())
			.seatId(seat.getSeatId())
			.expiresAt(null) // todo 결제 완료, 만료 기한 없음 (null? 아니면 결제 정보에서 받아서?)
			.build();

		reservation.complete(LocalDateTime.now());

		return reservation;
	}
}
