package com.back.b2st.domain.lottery.result.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.b2st.domain.lottery.result.dto.LotteryPaymentInfo;
import com.back.b2st.domain.lottery.result.entity.LotteryResult;

public interface LotteryResultRepository extends JpaRepository<LotteryResult, Long> {

	/**
	 * 결제를 위한 응모 정보 조회
	 * @param uuid
	 * @param memberId
	 * @return    응모자 id, 신청 등급, 신청 수
	 */
	@Query("""
		select new com.back.b2st.domain.lottery.result.dto.LotteryPaymentInfo(
				lr.memberId, le.grade, le.quantity
				)
		FROM LotteryResult lr
		JOIN LotteryEntry le ON lr.lotteryEntryId = le.id
		WHERE lr.uuid = :uuid
		  AND lr.memberId = :memberId
		""")
	LotteryPaymentInfo findPaymentInfoByid(
		@Param("uuid") UUID uuid,
		@Param("memberId") Long memberId);
}
