package com.back.b2st.domain.reservation.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.reservation.dto.request.ReservationReq;
import com.back.b2st.domain.reservation.dto.response.ReservationCreateRes;
import com.back.b2st.domain.reservation.dto.response.ReservationDetailRes;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationStatus;
import com.back.b2st.domain.reservation.error.ReservationErrorCode;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.scheduleseat.service.ScheduleSeatStateService;
import com.back.b2st.domain.scheduleseat.service.SeatHoldTokenService;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationService {

	private final ReservationRepository reservationRepository;

	private final SeatHoldTokenService seatHoldTokenService;
	private final ScheduleSeatStateService scheduleSeatStateService;

	/** === 예매 생성(결제 시작) === */
	@Transactional
	public ReservationCreateRes createReservation(Long memberId, ReservationReq request) {

		Long scheduleId = request.scheduleId();
		Long seatId = request.seatId();

		// 1) 활성 상태 중복 예매 방지(최소변경)
		if (reservationRepository.existsByScheduleIdAndSeatIdAndStatusIn(
			scheduleId,
			seatId,
			List.of(ReservationStatus.PENDING, ReservationStatus.COMPLETED)
		)) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_ALREADY_EXISTS);
		}

		// 2) HOLD 소유권 검증 (Redis)
		seatHoldTokenService.validateOwnership(scheduleId, seatId, memberId);

		// 3) DB 좌석 상태 검증 (HOLD + 만료)
		scheduleSeatStateService.validateHoldState(scheduleId, seatId); // [MOD]

		// 4) Reservation(PENDING) 생성
		Reservation reservation = request.toEntity(memberId);

		// 5) 저장 응답
		return ReservationCreateRes.from(reservationRepository.save(reservation));
	}

	/** === 예매 확정 (결제에서 호출되어야 함) === */
	@Transactional
	public void completeReservation(Long reservationId) {

		Reservation reservation = getReservation(reservationId);

		if (reservation.getStatus() == ReservationStatus.COMPLETED) {
			return;
		}

		if (!reservation.getStatus().canComplete()) {
			throw new BusinessException(ReservationErrorCode.INVALID_RESERVATION_STATUS);
		}

		reservation.complete(LocalDateTime.now());

		// 좌석 상태 변경 (HOLD → SOLD)
		scheduleSeatStateService.changeToSold(
			reservation.getScheduleId(),
			reservation.getSeatId()
		);

		seatHoldTokenService.remove(reservation.getScheduleId(), reservation.getSeatId());
	}

	/** === 결제 실패 (결제에서 호출되어야 함) === */
	@Transactional
	public void failReservation(Long reservationId) {

		Reservation reservation = getReservation(reservationId);

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

		// 좌석 복구 (HOLD → AVAILABLE)
		scheduleSeatStateService.changeToAvailable(
			reservation.getScheduleId(),
			reservation.getSeatId()
		);

		seatHoldTokenService.remove(reservation.getScheduleId(), reservation.getSeatId());
	}

	/** === 예매 취소 (일단 결제 완료 시 취소 불가) === */
	@Transactional
	public void cancelReservation(Long reservationId, Long memberId) {

		Reservation reservation = getMyReservation(reservationId, memberId);

		if (!reservation.getStatus().canCancel()) {
			throw new BusinessException(ReservationErrorCode.INVALID_RESERVATION_STATUS);
		}

		reservation.cancel(LocalDateTime.now());

		// 좌석 상태 복구 (HOLD → AVAILABLE) TODO: 일단 HOLD만 가능
		scheduleSeatStateService.changeToAvailable(
			reservation.getScheduleId(),
			reservation.getSeatId()
		);

		seatHoldTokenService.remove(reservation.getScheduleId(), reservation.getSeatId());
	}

	/** === 예매 만료 === */
	@Transactional
	public void expireReservation(Long reservationId) {

		Reservation reservation = getReservation(reservationId);

		if (!reservation.getStatus().canExpire()) {
			return;
		}

		reservation.expire();

		// 좌석 상태 복구 (HOLD → AVAILABLE)
		scheduleSeatStateService.changeToAvailable(
			reservation.getScheduleId(),
			reservation.getSeatId()
		);

		seatHoldTokenService.remove(reservation.getScheduleId(), reservation.getSeatId());
	}

	/** === 예매 조회 === */
	@Transactional(readOnly = true)
	public ReservationDetailRes getReservationDetail(Long reservationId, Long memberId) {

		ReservationDetailRes result =
			reservationRepository.findReservationDetail(reservationId, memberId);

		if (result == null) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
		}

		return result;
	}

	/** === 예매 다건 조회 === */
	@Transactional(readOnly = true)
	public List<ReservationDetailRes> getMyReservationsDetail(Long memberId) {
		return reservationRepository.findMyReservationDetails(memberId);
	}

	// === 공통 유틸 === //
	private Reservation getReservation(Long reservationId) {
		return reservationRepository.findById(reservationId)
			.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));
	}

	private Reservation getMyReservation(Long reservationId, Long memberId) {
		Reservation reservation = getReservation(reservationId);

		if (!reservation.getMemberId().equals(memberId)) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_FORBIDDEN);
		}
		return reservation;
	}

}
