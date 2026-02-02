package com.back.b2st.domain.reservation.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.reservation.dto.request.ReservationReq;
import com.back.b2st.domain.reservation.dto.response.ReservationCreateRes;
import com.back.b2st.domain.reservation.dto.response.SeatReservationResult;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationStatus;
import com.back.b2st.domain.reservation.error.ReservationErrorCode;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.ticket.service.TicketService;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationService {

	private final ReservationRepository reservationRepository;
	private final ReservationSeatManager reservationSeatManager;

	private final TicketService ticketService;

	/** === 예매 생성(결제 시작) === */
	@Transactional
	public ReservationCreateRes createReservation(Long memberId, ReservationReq request) {

		Long scheduleId = request.scheduleId();
		List<Long> seatIds = request.seatIds();

		// 1. 좌석은 1자리씩 예매 가능
		validateReservationPolicy(seatIds);

		// 2. 좌석 검사 + 만료시각 확보
		SeatReservationResult seatResult =
			reservationSeatManager.prepareSeatReservation(
				scheduleId, seatIds, memberId
			);

		// 3. 예매 중복 검증
		validateReservationDuplicate(scheduleId, seatResult.scheduleSeatIds());

		// 4. Reservation(PENDING) 생성
		Reservation reservation =
			reservationRepository.save(
				request.toEntity(memberId, seatResult.expiresAt()));

		// 5. 좌석 귀속
		reservationSeatManager.attachSeats(
			reservation.getId(),
			seatResult.scheduleSeatIds()
		);

		return ReservationCreateRes.from(reservation);
	}

	private static void validateReservationPolicy(List<Long> seatIds) {
		if (seatIds.size() != 1) {
			throw new BusinessException(ReservationErrorCode.INVALID_SEAT_COUNT);
		}
	}

	/** === 결제 실패 (결제에서 호출되어야 함) === */
	@Transactional
	public void failReservation(Long reservationId) {

		Reservation reservation = getReservationWithLock(reservationId);

		if (reservation.getStatus() == ReservationStatus.FAILED) {
			return;
		}

		if (reservation.getStatus() == ReservationStatus.COMPLETED) {
			return;
		}

		if (!reservation.getStatus().canFail()) {
			throw new BusinessException(ReservationErrorCode.INVALID_RESERVATION_STATUS);
		}

		// PENDING -> FAILED
		reservation.fail();

		// 좌석 상태 복구 (HOLD → AVAILABLE)
		reservationSeatManager.releaseAllSeats(reservationId);
	}

	/** === 예매 취소 (일단 결제 완료 시 취소 불가) === */
	@Transactional
	public void cancelReservation(Long reservationId, Long memberId) {

		Reservation reservation = getMyReservationWithLock(reservationId, memberId);

		if (!reservation.getStatus().canCancel()) {
			throw new BusinessException(ReservationErrorCode.INVALID_RESERVATION_STATUS);
		}

		//TODO: 환불, 결제 취소 로직

		// 1) 티켓 취소 (ISSUED -> CANCELED)
		ticketService.cancelTicketsByReservation(reservationId, memberId);

		// 2) 예매 취소 (COMPLETED -> CANCELLED)
		reservation.cancel(LocalDateTime.now());

		// 3) 좌석 해제 (SOLD -> AVAILABLE)
		reservationSeatManager.releaseForceAllSeats(reservationId);
	}

	/** === PENDING 만료 배치 처리 (스케줄러) === */
	@Transactional
	public int expirePendingReservationsBatch() {
		LocalDateTime now = LocalDateTime.now();

		List<Long> expiredIds = reservationRepository.findExpiredPendingIds(ReservationStatus.PENDING, now);
		if (expiredIds.isEmpty()) {
			return 0;
		}

		return reservationRepository.bulkExpirePendingByIds(
			expiredIds,
			ReservationStatus.PENDING,
			ReservationStatus.EXPIRED
		);
	}

	/** === 예매 만료 (일단 안 씀) === */
	@Transactional
	public void expireReservation(Long reservationId) {

		Reservation reservation = getReservationWithLock(reservationId);

		LocalDateTime now = LocalDateTime.now();
		if (!reservation.getStatus().canExpire()) {
			return;
		}
		if (reservation.getExpiresAt() == null || reservation.getExpiresAt().isAfter(now)) {
			return;
		}

		reservation.expire();

		// 좌석 상태 복구 (HOLD → AVAILABLE)
		reservationSeatManager.releaseAllSeats(reservationId);
	}

	/** === 예매 확정 (일단 안 씀) === */
	// TODO: 지금 안 씀
	@Transactional
	public void completeReservation(Long reservationId) {

		Reservation reservation = getReservationWithLock(reservationId);

		if (reservation.getStatus() == ReservationStatus.COMPLETED) {
			ticketService.ensureTicketsForReservation(reservationId);
			return;
		}

		if (!reservation.getStatus().canComplete()) {
			throw new BusinessException(ReservationErrorCode.INVALID_RESERVATION_STATUS);
		}

		reservation.complete(LocalDateTime.now());

		// 좌석 상태 변경 (HOLD → SOLD)
		reservationSeatManager.confirmAllSeats(reservationId);

		ticketService.ensureTicketsForReservation(reservationId);
	}

	// === 중복 예매 방지 === //
	private void validateReservationDuplicate(Long scheduleId, List<Long> scheduleSeatIds) {

		LocalDateTime now = LocalDateTime.now();

		for (Long scheduleSeatId : scheduleSeatIds) {

			// 1. 이미 완료된 예매 존재
			if (reservationRepository.existsCompletedByScheduleSeat(
				scheduleId,
				scheduleSeatId
			)) {
				throw new BusinessException(ReservationErrorCode.RESERVATION_ALREADY_EXISTS);
			}

			// 2. 살아있는 PENDING 예매 존재
			if (reservationRepository.existsActivePendingByScheduleSeat(
				scheduleId,
				scheduleSeatId,
				now
			)) {
				throw new BusinessException(ReservationErrorCode.RESERVATION_ALREADY_EXISTS);
			}
		}
	}

	// === 공통 유틸 (락) === //
	private Reservation getReservationWithLock(Long reservationId) {
		return reservationRepository.findByIdWithLock(reservationId)
			.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));
	}

	private Reservation getMyReservationWithLock(Long reservationId, Long memberId) {
		Reservation reservation = getReservationWithLock(reservationId);
		if (!reservation.getMemberId().equals(memberId)) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_FORBIDDEN);
		}
		return reservation;
	}
}
