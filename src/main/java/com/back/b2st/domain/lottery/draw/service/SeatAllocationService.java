package com.back.b2st.domain.lottery.draw.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.lottery.result.dto.LotteryReservationInfo;
import com.back.b2st.domain.lottery.result.repository.LotteryResultRepository;
import com.back.b2st.domain.performanceschedule.dto.DrawTargetPerformance;
import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.reservation.repository.ReservationSeatRepository;
import com.back.b2st.domain.reservation.service.LotteryReservationService;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.ticket.service.TicketService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatAllocationService {

	private final ScheduleSeatRepository scheduleSeatRepository;
	private final LotteryResultRepository lotteryResultRepository;
	private final LotteryReservationService lotteryReservationService;
	private final PerformanceScheduleRepository performanceScheduleRepository;
	private final ReservationSeatRepository reservationSeatRepository;
	private final TicketService ticketService;

	/**
	 * 공연 시작 3일전 조회
	 */
	public List<DrawTargetPerformance> findBookingOpenPerformances() {
		LocalDateTime startDate = LocalDate.now().plusDays(3).atStartOfDay();
		LocalDateTime endDate = LocalDate.now().plusDays(3).atTime(23, 59, 59);

		return performanceScheduleRepository.findByOpenBetween(startDate, endDate);
	}

	/**
	 * 응모 정보 조회
	 */
	public List<LotteryReservationInfo> findReservationInfos(Long scheduleId) {
		return lotteryResultRepository.findReservationInfoByPaidIsTrue(scheduleId);
	}

	/**
	 * 예매 생성 여부 조회
	 */
	public boolean findReservation(Long reservationId) {
		return reservationSeatRepository.existsByReservationId(reservationId);
	}

	public void allocateSeats(Long scheduleId) {
		// 당첨자 중 결제가 완료된 고객 리스트 조회
		List<LotteryReservationInfo> reservationInfos = findReservationInfos(scheduleId);

		// 좌석할당
		for (LotteryReservationInfo infos : reservationInfos) {
			try {
				allocateSeatsForLottery(infos);
				log.debug("좌석 할당 - reservationId: {}, memberId: {}", infos.reservationId(), infos.memberId());
			} catch (Exception e) {
				log.error("좌석 할당 실패 reservationId: {}, memberId: {}", infos.reservationId(), infos.memberId(), e);
			}
		}

		// 회차 - 좌석배치 완료
		PerformanceSchedule schedule = performanceScheduleRepository.findById(scheduleId).orElseThrow();
		schedule.markSeatAllocated();
	}

	@Transactional
	public List<ScheduleSeat> allocateSeatsForLottery(LotteryReservationInfo info) {
		// 배치 완료된 예매
		if (findReservation(info.reservationId())) {
			log.debug("이미 좌석 배치 완료 - reservationId: {}", info.reservationId());
			return List.of();
		}

		List<ScheduleSeat> availableSeats = getAvailableSeats(info);

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

		List<Long> seatsIds = allocatedSeats.stream()
			.map(ScheduleSeat::getId)
			.toList();

		// 좌석 확정 + 예매 매핑
		lotteryReservationService.confirmAssignedSeats(info.reservationId(), info.scheduleId(), seatsIds);

		log.info("좌석 배정 완료 - resultId: {}, memberId: {}, 배정 좌석 수: {}",
			info.resultId(), info.memberId(), allocatedSeats.size());

		// 티켓 생성
		for (ScheduleSeat seat : allocatedSeats) {
			ticketService.createTicket(info.reservationId(), info.memberId(), seat.getSeatId());
		}

		return allocatedSeats;
	}

	/**
	 * 예매 가능 좌석 조회(AVAILABLE)
	 */
	private List<ScheduleSeat> getAvailableSeats(LotteryReservationInfo info) {
		return scheduleSeatRepository.findAvailableSeatsByGrade(
			info.scheduleId(),
			info.grade());
	}
}
