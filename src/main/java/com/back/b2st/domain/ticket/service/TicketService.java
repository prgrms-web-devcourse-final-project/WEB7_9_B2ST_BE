package com.back.b2st.domain.ticket.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.b2st.domain.performanceschedule.entity.PerformanceSchedule;
import com.back.b2st.domain.performanceschedule.repository.PerformanceScheduleRepository;
import com.back.b2st.domain.prereservation.booking.entity.PrereservationBooking;
import com.back.b2st.domain.prereservation.booking.repository.PrereservationBookingRepository;
import com.back.b2st.domain.reservation.entity.Reservation;
import com.back.b2st.domain.reservation.dto.response.ReservationSeatInfo;
import com.back.b2st.domain.reservation.repository.ReservationRepository;
import com.back.b2st.domain.reservation.repository.ReservationSeatRepository;
import com.back.b2st.domain.scheduleseat.entity.ScheduleSeat;
import com.back.b2st.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.back.b2st.domain.seat.seat.entity.Seat;
import com.back.b2st.domain.seat.seat.repository.SeatRepository;
import com.back.b2st.domain.ticket.dto.response.TicketRes;
import com.back.b2st.domain.ticket.entity.AcquisitionType;
import com.back.b2st.domain.ticket.entity.Ticket;
import com.back.b2st.domain.ticket.entity.TicketStatus;
import com.back.b2st.domain.ticket.error.TicketErrorCode;
import com.back.b2st.domain.ticket.repository.TicketRepository;
import com.back.b2st.domain.trade.entity.Trade;
import com.back.b2st.domain.trade.entity.TradeStatus;
import com.back.b2st.domain.trade.repository.TradeRepository;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

	private final TicketRepository ticketRepository;
	private final SeatRepository seatRepository;
	private final ReservationRepository reservationRepository;
	private final PerformanceScheduleRepository performanceScheduleRepository;
	private final ReservationSeatRepository reservationSeatRepository;
	private final PrereservationBookingRepository prereservationBookingRepository;
	private final ScheduleSeatRepository scheduleSeatRepository;
	private final TradeRepository tradeRepository;

	public Ticket createTicket(Long reservationId, Long memberId, Long seatId) {
		return ticketRepository.findByReservationIdAndMemberIdAndSeatId(reservationId, memberId, seatId)
			.orElseGet(() -> {
				// TODO QR코드 로직
				Ticket ticket = Ticket.builder()
					.reservationId(reservationId)
					.memberId(memberId)
					.seatId(seatId)
					.build();
				try {
					return ticketRepository.save(ticket);
				} catch (DataIntegrityViolationException e) {
					return ticketRepository.findByReservationIdAndMemberIdAndSeatId(reservationId, memberId, seatId)
						.orElseThrow(() -> e);
				}
			});
	}

	private Ticket getTicket(Long reservationId, Long memberId, Long seatId) {
		return ticketRepository.findByReservationIdAndMemberIdAndSeatId(reservationId, memberId, seatId)
			.orElseThrow(() -> new BusinessException(TicketErrorCode.TICKET_NOT_FOUND));
	}

	public Ticket getTicketById(Long ticketId) {
		return ticketRepository.findById(ticketId)
			.orElseThrow(() -> new BusinessException(TicketErrorCode.TICKET_NOT_FOUND));
	}

	@Transactional
	public Ticket cancelTicket(Long reservationId, Long memberId, Long seatId) {
		Ticket ticket = getTicket(reservationId, memberId, seatId);

		switch (ticket.getStatus()) {
			case ISSUED -> ticket.cancel();
			case CANCELED -> throw new BusinessException(TicketErrorCode.ALREADY_CANCEL_TICKET);
			default -> throw new BusinessException(TicketErrorCode.TICKET_NOT_CANCELABLE);
		}

		return ticket;
	}

	@Transactional
	public Ticket useTicket(Long reservationId, Long memberId, Long seatId) {
		Ticket ticket = getTicket(reservationId, memberId, seatId);
		ticket.use();

		return ticket;
	}

	@Transactional
	public Ticket exchangeTicket(Long reservationId, Long memberId, Long seatId) {
		Ticket ticket = getTicket(reservationId, memberId, seatId);
		ticket.exchange();

		return ticket;
	}

	@Transactional
	public Ticket transferTicket(Long reservationId, Long memberId, Long seatId) {
		Ticket ticket = getTicket(reservationId, memberId, seatId);
		ticket.transfer();

		return ticket;
	}

	@Transactional
	public Ticket expireTicket(Long reservationId, Long memberId, Long seatId) {
		Ticket ticket = getTicket(reservationId, memberId, seatId);
		ticket.expire();

		return ticket;
	}

	@Transactional
	public Ticket restoreTicket(Long ticketId) {
		Ticket ticket = getTicketById(ticketId);
		if (ticket.getStatus() != TicketStatus.TRANSFERRED) {
			throw new BusinessException(TicketErrorCode.TICKET_NOT_TRANSFERABLE);
		}
		ticket.restore();

		return ticket;
	}

	public List<TicketRes> getMyTickets(Long memberId) {
		List<Ticket> tickets = ticketRepository.findByMemberId(memberId);

		// 구매자로 받은 완료된 거래 조회 (교환/양도)
		List<Trade> completedTrades = tradeRepository.findAllByBuyerIdAndStatus(memberId, TradeStatus.COMPLETED);

		return tickets.stream()
			.map(ticket -> {
				Seat seat = seatRepository.findById(ticket.getSeatId())
					.orElseThrow(() -> new BusinessException(TicketErrorCode.TICKET_NOT_FOUND));
				PerformanceSchedule schedule = resolveScheduleForTicket(ticket);

				// 티켓 획득 경로 판단
				AcquisitionType acquisitionType = determineAcquisitionType(ticket, completedTrades);

				return TicketRes.builder()
					.ticketId(ticket.getId())
					.reservationId(ticket.getReservationId())
					.seatId(ticket.getSeatId())
					.status(ticket.getStatus())
					.sectionName(seat.getSectionName())
					.rowLabel(seat.getRowLabel())
					.seatNumber(seat.getSeatNumber())
					.performanceId(schedule.getPerformance().getPerformanceId())
					.acquisitionType(acquisitionType)
					.build();
			})
			.collect(Collectors.toList());
	}

	/**
	 * 티켓의 획득 경로를 판단합니다.
	 * @param ticket 현재 티켓
	 * @param completedTrades 구매자로 받은 완료된 거래 목록
	 * @return AcquisitionType (RESERVATION, TRANSFER, EXCHANGE)
	 */
	private AcquisitionType determineAcquisitionType(Ticket ticket, List<Trade> completedTrades) {
		// 완료된 거래 중에서 현재 티켓의 좌석과 일치하는 거래 찾기
		for (Trade trade : completedTrades) {
			// Trade의 원본 티켓 조회
			Ticket originalTicket = ticketRepository.findById(trade.getTicketId()).orElse(null);
			if (originalTicket != null && originalTicket.getSeatId().equals(ticket.getSeatId())) {
				// 같은 좌석이면 교환 또는 양도로 받은 티켓
				return trade.getType() == com.back.b2st.domain.trade.entity.TradeType.TRANSFER
					? AcquisitionType.TRANSFER
					: AcquisitionType.EXCHANGE;
			}
		}

		// 거래 이력이 없으면 예매로 받은 티켓
		return AcquisitionType.RESERVATION;
	}

	private PerformanceSchedule resolveScheduleForTicket(Ticket ticket) {
		Reservation reservation = reservationRepository.findById(ticket.getReservationId()).orElse(null);

		if (reservation != null) {
			List<ReservationSeatInfo> reservationSeats = reservationSeatRepository.findSeatInfos(reservation.getId());
			boolean isSeatPartOfReservation = reservationSeats.stream()
				.anyMatch(seatInfo -> seatInfo.seatId().equals(ticket.getSeatId()));

			if (isSeatPartOfReservation) {
				return performanceScheduleRepository.findById(reservation.getScheduleId())
					.orElseThrow(() -> new BusinessException(TicketErrorCode.TICKET_NOT_FOUND));
			}
		}

		PrereservationBooking booking = prereservationBookingRepository.findById(ticket.getReservationId()).orElse(null);
		if (booking != null) {
			ScheduleSeat scheduleSeat = scheduleSeatRepository.findById(booking.getScheduleSeatId())
				.orElseThrow(() -> new BusinessException(TicketErrorCode.TICKET_NOT_FOUND));

			if (scheduleSeat.getSeatId().equals(ticket.getSeatId())) {
				return performanceScheduleRepository.findById(booking.getScheduleId())
					.orElseThrow(() -> new BusinessException(TicketErrorCode.TICKET_NOT_FOUND));
			}
		}

		// 레거시/데이터 불일치 케이스: 기존 동작처럼 reservationId로 scheduleId를 역추적한다.
		if (reservation != null) {
			return performanceScheduleRepository.findById(reservation.getScheduleId())
				.orElseThrow(() -> new BusinessException(TicketErrorCode.TICKET_NOT_FOUND));
		}

		throw new BusinessException(TicketErrorCode.TICKET_NOT_FOUND);
	}

	@Transactional
	public void cancelTicketsByReservation(Long reservationId, Long memberId) {

		List<Ticket> tickets =
			ticketRepository.findAllByReservationIdAndMemberId(reservationId, memberId);

		if (tickets.isEmpty()) {
			return;
		}

		for (Ticket ticket : tickets) {
			switch (ticket.getStatus()) {
				case ISSUED -> ticket.cancel();
				case CANCELED -> {
				}
				default -> throw new BusinessException(TicketErrorCode.TICKET_NOT_CANCELABLE);
			}
		}
	}

}
