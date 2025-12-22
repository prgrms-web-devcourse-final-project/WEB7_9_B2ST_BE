package com.back.b2st.domain.reservation.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.reservation.dto.request.ReservationReq;
import com.back.b2st.domain.reservation.dto.response.ReservationDetailRes;
import com.back.b2st.domain.reservation.dto.response.ReservationRes;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.entity.ReservationStatus;
import com.back.b2st.domain.reservation.error.ReservationErrorCode;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.entity.SeatStatus;
import com.back.b2st.domain.scheduleseat.error.ScheduleSeatErrorCode;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.scheduleseat.service.ScheduleSeatStateService;
import com.back.b2st.domain.scheduleseat.service.SeatHoldTokenService;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationService {

	private final ReservationRepository reservationRepository;
	private final ScheduleSeatRepository scheduleSeatRepository;

	private final SeatHoldTokenService seatHoldTokenService;
	private final ScheduleSeatStateService scheduleSeatStateService;

	/** === 예매 생성 === */
	@Transactional
	public ReservationDetailRes createReservation(Long memberId, ReservationReq request) {

		Long scheduleId = request.scheduleId();
		Long seatId = request.seatId();

		// 1. HOLD 소유권 검증 (Redis)
		seatHoldTokenService.validateOwnership(scheduleId, seatId, memberId);

		// 2) DB 좌석 상태 검증 (HOLD + 만료 체크) TODO: 이건 좌석쪽에서 검증해야 할 것 같은데
		ScheduleSeat scheduleSeat = scheduleSeatRepository
			.findByScheduleIdAndSeatId(scheduleId, seatId)
			.orElseThrow(() -> new BusinessException(ScheduleSeatErrorCode.SEAT_NOT_FOUND));

		if (scheduleSeat.getStatus() != SeatStatus.HOLD) {
			throw new BusinessException(ScheduleSeatErrorCode.SEAT_NOT_HOLD);
		}

		if (scheduleSeat.getHoldExpiredAt() != null && scheduleSeat.getHoldExpiredAt().isBefore(LocalDateTime.now())) {
			throw new BusinessException(ScheduleSeatErrorCode.SEAT_HOLD_EXPIRED);
		}

		// 3) 좌석 중복 예매 방지 (Order 단에서 1차 방어) TODO:
		if (reservationRepository.existsByScheduleIdAndSeatId(scheduleId, seatId)) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_ALREADY_EXISTS);
		}

		// 2. Reservation 생성
		Reservation reservation = request.toEntity(memberId);

		// 3. 저장
		Reservation saved = reservationRepository.save(reservation);

		// 4. 반환 TODO: 이걸로 반환을 안 해도 될 것 같음 -> 결제로 넘겨주니까
		return getReservationDetail(saved.getId(), memberId);
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

	/** === 예매 확정 === */
	@Transactional
	public void completeReservation(Long reservationId, Long memberId) {

		Reservation reservation = getReservation(reservationId);

		// 소유자 검증(임시 안전장치: 최종적으로는 PG webhook/관리자 confirm에서만 호출)
		if (!reservation.getMemberId().equals(memberId)) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_FORBIDDEN);
		}

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

	/** === 결제 실패(카드 등): PENDING -> FAILED + 좌석 복구 === */
	@Transactional
	public void failReservation(Long reservationId, Long memberId) { // [MOD] 카드 실패 처리(최소형)

		Reservation reservation = getReservation(reservationId);

		if (!reservation.getMemberId().equals(memberId)) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_FORBIDDEN);
		}

		if (!reservation.getStatus().canFail()) {
			throw new BusinessException(ReservationErrorCode.INVALID_RESERVATION_STATUS);
		}

		reservation.fail();

		// 좌석 복구 (HOLD → AVAILABLE)
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

	/** === 예매 단건 조회 === */
	@Transactional(readOnly = true)
	public ReservationRes getReservation(Long reservationId, Long memberId) {

		Reservation reservation = getReservation(reservationId);

		if (!reservation.getMemberId().equals(memberId)) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_FORBIDDEN);
		}

		return ReservationRes.from(reservation);
	}

	/** === 예매 전체 조회 === */
	@Transactional(readOnly = true)
	public List<ReservationRes> getMyReservations(Long memberId) {
		List<Reservation> reservations = reservationRepository.findAllByMemberId(memberId);
		return ReservationRes.fromList(reservations);
	}

	/** === 예매 상세 조회 === */
	@Transactional(readOnly = true)
	public ReservationDetailRes getReservationDetail(Long reservationId, Long memberId) {

		ReservationDetailRes result =
			reservationRepository.findReservationDetail(reservationId, memberId);

		if (result == null) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
		}

		return result;
	}

	/** === 예매 상세 다건 조회 === */
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
		Reservation reservation = reservationRepository.findById(reservationId)
			.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

		if (!reservation.getMemberId().equals(memberId)) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_FORBIDDEN);
		}
		return reservation;
	}

}
