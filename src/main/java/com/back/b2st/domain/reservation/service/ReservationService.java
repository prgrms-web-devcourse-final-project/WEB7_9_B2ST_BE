package com.back.b2st.domain.reservation.service;

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
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.scheduleseat.service.ScheduleSeatCommandService;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationService {

	private final ReservationRepository reservationRepository;
	private final ScheduleSeatRepository scheduleSeatRepository;

	private final SeatLockService seatLockService;
	private final ScheduleSeatCommandService scheduleSeatCommandService;
	private final SeatHoldTokenService seatHoldTokenService;

	/** === 예매 생성 === */
	@Transactional
	public Long reserveSeat(Long memberId, ReservationReq request) {

		Long scheduleId = request.scheduleId();
		Long seatId = request.seatId();

		// 1. 락 걸기
		String lockValue = seatLockService.tryLock(scheduleId, seatId, memberId);

		if (lockValue == null) {
			throw new BusinessException(ReservationErrorCode.SEAT_LOCK_FAILED);
		}

		try {
			// 2. AVAILABLE → HOLD
			scheduleSeatCommandService.holdSeat(scheduleId, seatId);

			// 3. Redis HOLD TTL 저장
			seatHoldTokenService.save(scheduleId, seatId, memberId);

			// 4. Reservation 생성
			Reservation reservation = request.toEntity(memberId);
			Reservation saved = reservationRepository.save(reservation);

			return saved.getId();

		} finally {
			seatLockService.unlock(scheduleId, seatId, lockValue);
		}

	}

	@Transactional
	public ReservationDetailRes createReservation(Long memberId, ReservationReq request) {
		Long reservationId = reserveSeat(memberId, request);
		return getReservationDetail(reservationId, memberId);
	}

	/** === 예매 취소 (일단 결제 완료 시 취소 불가) === */
	@Transactional
	public void cancelReservation(Long reservationId, Long memberId) {

		Reservation reservation = reservationRepository.findById(reservationId)
			.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

		// 본인 확인
		if (!reservation.getMemberId().equals(memberId)) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_FORBIDDEN);
		}

		// 이미 결제 완료된 건 취소 불가 TODO: 추후 논의
		if (reservation.getStatus() == ReservationStatus.COMPLETED) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_ALREADY_COMPLETED);
		}

		// 상태 변경
		reservation.cancel();

		// 좌석 AVAILABLE로 복구
		scheduleSeatRepository
			.findByScheduleIdAndSeatId(reservation.getScheduleId(), reservation.getSeatId())
			.ifPresent(ScheduleSeat::release);
	}

	/** === 예매 확정 === */
	@Transactional
	public void completeReservation(Long reservationId, Long memberId) {

		Reservation reservation = reservationRepository.findById(reservationId)
			.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

		// 본인 확인
		if (!reservation.getMemberId().equals(memberId)) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_FORBIDDEN);
		}

		// 이미 취소된 예매는 결제 완료 불가
		if (reservation.getStatus() == ReservationStatus.CANCELED) {
			throw new BusinessException(ReservationErrorCode.RESERVATION_ALREADY_CANCELED);
		}

		// 결제 완료 종료
		if (reservation.getStatus() == ReservationStatus.COMPLETED) {
			return;
		}

		// 상태 변경
		reservation.complete();

		// 좌석 SOLD 변경
		scheduleSeatRepository
			.findByScheduleIdAndSeatId(reservation.getScheduleId(), reservation.getSeatId())
			.ifPresent(ScheduleSeat::sold);
	}

	/** === 예매 단건 조회 === */
	@Transactional(readOnly = true)
	public ReservationRes getReservation(Long reservationId, Long memberId) {

		Reservation reservation = reservationRepository.findById(reservationId)
			.orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));

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

}
