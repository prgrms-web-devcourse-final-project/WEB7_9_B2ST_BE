package com.back.b2st.domain.lottery.result.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.b2st.domain.lottery.result.dto.LotteryPaymentInfo;
import com.back.b2st.domain.lottery.result.dto.LotteryReservationInfo;
import com.back.b2st.domain.lottery.result.dto.LotteryResultEmailInfo;
import com.back.b2st.domain.lottery.result.entity.LotteryResult;

public interface LotteryResultRepository extends JpaRepository<LotteryResult, Long> {

	/**
	 * 결제를 위한 응모 정보 조회
	 * @param uuid    추첨 응모의 uuid
	 * @return    응모자 id, 신청 등급, 신청 수
	 */
	@Query("""
		select new com.back.b2st.domain.lottery.result.dto.LotteryPaymentInfo(
				lr.id, lr.memberId, le.grade, le.quantity
				)
		FROM LotteryResult lr
		JOIN LotteryEntry le ON lr.lotteryEntryId = le.id
		WHERE le.uuid = :uuid
		""")
	LotteryPaymentInfo findPaymentInfoByid(    // todo ById
		@Param("uuid") UUID uuid);

	/**
	 * 이메일 전송을 위한 미결제자 정보 조회
	 */
	@Query("""
		select new com.back.b2st.domain.lottery.result.dto.LotteryResultEmailInfo(
				lr.id, lr.memberId, m.name, le.grade, le.quantity, lr.paymentDeadline
		)
		FROM LotteryResult lr
		JOIN LotteryEntry le ON lr.lotteryEntryId = le.id
		JOIN Member m ON lr.memberId = m.id
		WHERE le.scheduleId = :scheduleId
		  AND lr.paid = false
		""")
	List<LotteryResultEmailInfo> findSendEmailInfoByScheduleId(
		@Param("scheduleId") Long scheduleId);

	/**
	 * 좌석 분배를 위한 예매에 필요한 정보 조회
	 */
	@Query("""
		select new com.back.b2st.domain.lottery.result.dto.LotteryReservationInfo(
				r.id, lr.id, lr.memberId, le.scheduleId, le.grade, le.quantity
				)
		FROM LotteryResult lr
		JOIN LotteryEntry le ON lr.lotteryEntryId = le.id
		JOIN Reservation r ON r.memberId = lr.memberId AND r.scheduleId = le.scheduleId
		WHERE lr.paid = true
		  AND le.scheduleId = :scheduleId
		""")
	List<LotteryReservationInfo> findReservationInfoByPaidIsTrue(Long scheduleId);
}
