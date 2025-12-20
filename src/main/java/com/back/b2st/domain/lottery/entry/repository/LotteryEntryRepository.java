package com.back.b2st.domain.lottery.entry.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.back.b2st.domain.lottery.entry.dto.response.AppliedLotteryInfo;
import com.back.b2st.domain.lottery.entry.entity.LotteryEntry;

public interface LotteryEntryRepository extends JpaRepository<LotteryEntry, Long> {
	boolean existsByMemberIdAndPerformanceIdAndScheduleId(Long memberId, Long performanceId, Long scheduleId);

	@Query("""
			SELECT new com.back.b2st.domain.lottery.entry.dto.response.AppliedLotteryInfo(
					le.uuid, p.title, ps.startAt, ps.roundNo, le.grade, le.quantity, le.status
			)
			FROM LotteryEntry le
			JOIN Performance p ON le.performanceId = p.performanceId
			JOIN PerformanceSchedule ps ON le.scheduleId = ps.performanceScheduleId
			WHERE le.memberId = :memberId
			  AND le.createdAt >= :month
			ORDER BY le.createdAt DESC
		""")
	List<AppliedLotteryInfo> findAppliedLotteryByMemberId(    // todo 페이징 추가
		@Param("memberId") Long memberId, @Param("month") LocalDateTime month);
}
