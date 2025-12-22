package com.back.b2st.domain.lottery.draw.dto;

/**
 * 가중치 적용 응모자 정보
 * @param applicantInfo
 * @param weight
 */
public record WeightedApplicant(
	LotteryApplicantInfo applicantInfo,
	int weight
) {
}
