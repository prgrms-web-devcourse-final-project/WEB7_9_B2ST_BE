package com.back.b2st.domain.member.dto.response;

public record DashboardStatsRes(
	long totalMembers,
	long activeMembers,
	long withdrawnMembers,
	long adminCount,
	long todaySignups,
	long todayLogins,
	long todayLoginFailures,
	int currentLockedAccounts
) {
}
