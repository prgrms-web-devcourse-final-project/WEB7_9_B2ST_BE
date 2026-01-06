package com.back.b2st.domain.lottery.entry.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.b2st.domain.lottery.draw.dto.LotteryApplicantInfo;
import com.back.b2st.domain.lottery.entry.dto.response.AppliedLotteryInfo;
import com.back.b2st.domain.lottery.entry.entity.LotteryEntry;

public interface LotteryEntryRepository extends JpaRepository<LotteryEntry, Long> {
	boolean existsByMemberIdAndPerformanceIdAndScheduleId(Long memberId, Long performanceId, Long scheduleId);

	@Query("""
			SELECT new com.back.b2st.domain.lottery.entry.dto.response.AppliedLotteryInfo(
					le.uuid, p.title, ps.startAt, ps.roundNo, le.grade, sg.price, le.quantity, le.status
			)
			FROM LotteryEntry le
			JOIN Performance p ON le.performanceId = p.performanceId
			JOIN PerformanceSchedule ps ON le.scheduleId = ps.performanceScheduleId
			JOIN SeatGrade sg ON sg.performanceId = p.performanceId AND sg.grade = le.grade
			WHERE le.memberId = :memberId
			  AND le.createdAt >= :month
			ORDER BY le.createdAt DESC
		""")
	Slice<AppliedLotteryInfo> findAppliedLotteryByMemberId(
		@Param("memberId") Long memberId,
		@Param("month") LocalDateTime month,
		Pageable pageable
	);

	List<Long> findByScheduleId(Long scheduleId);

	/**
	 * 신청 정보 확인
	 * @param performanceScheduleId
	 * @return
	 */
	@Query("""
		select new com.back.b2st.domain.lottery.draw.dto.LotteryApplicantInfo(
				le.id, le.memberId, le.grade, le.quantity
		)
		from LotteryEntry le
		where le.scheduleId = :scheduleId
		""")
	List<LotteryApplicantInfo> findAppliedInfoByScheduleId(@Param("scheduleId") Long performanceScheduleId);

	/**
	 * 신청 정보 단 건 조회 - id
	 * @param id
	 * @return
	 */
	@Query("""
		select new com.back.b2st.domain.lottery.draw.dto.LotteryApplicantInfo(
				le.id, le.memberId, le.grade, le.quantity
		)
		from LotteryEntry le
		where le.id = :id 
		""")
	Optional<LotteryApplicantInfo> findAppliedInfoByid(@Param("id") Long id);

	/**
	 * 당첨, 낙첨 추첨 결과 업데이트
	 */
	@Modifying(clearAutomatically = true)
	@Query("""
		update LotteryEntry le
		set le.status =
			case when le.id in :winnerIds then com.back.b2st.domain.lottery.entry.entity.LotteryStatus.WIN
				else com.back.b2st.domain.lottery.entry.entity.LotteryStatus.LOSE
			end
		where le.scheduleId = :scheduleId
		""")
	int updateStatusBySchedule(
		@Param("scheduleId") Long scheduleId,
		@Param("winnerIds") List<Long> winnerIds
	);

	// test
	LotteryEntry findByUuid(UUID uuid);
}
