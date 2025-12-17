package com.back.b2st.domain.seat.grade.entity;

import com.back.b2st.domain.seat.grade.error.SeatGradeErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

public enum SeatGradeType {
	STANDARD,    // 기본
	VIP,
	ROYAL,
	SUPERIOR,
	A,
	B,
	RESTRICTED_VIEW;   // 시야제한석

	public static SeatGradeType fromString(String grade) {
		if (grade == null || grade.isBlank()) {
			throw new BusinessException(SeatGradeErrorCode.GRADE_REQUIRED);
		}

		try {
			return SeatGradeType.valueOf(grade.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new BusinessException(SeatGradeErrorCode.INVALID_GRADE_TYPE);
		}
	}
}