package com.back.b2st.domain.seat.grade.entity;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.back.b2st.domain.seat.grade.error.SeatGradeErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.Getter;

@Getter
public enum SeatGradeType {
	STANDARD(1, "일반석"),
	VIP(2, "VIP석"),
	ROYAL(3, "로열석"),
	SUPERIOR(4, "S석"),
	A(5, "A석"),
	B(6, "B석"),
	RESTRICTED_VIEW(7, "시야제한석");

	private final int id;
	private final String displayName;

	// 조회를 위한 Map
	private static final Map<Integer, SeatGradeType> ID_MAP;
	private static final Map<String, SeatGradeType> DISPLAY_NAME_MAP;

	static {
		ID_MAP = Arrays.stream(values())
			.collect(Collectors.toMap(
				SeatGradeType::getId,
				Function.identity()
			));

		DISPLAY_NAME_MAP = Arrays.stream(values())
			.collect(Collectors.toMap(
				SeatGradeType::getDisplayName,
				Function.identity()
			));
	}

	SeatGradeType(int id, String displayName) {
		this.id = id;
		this.displayName = displayName;
	}

	// 검증
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

	// 조회 id
	public static SeatGradeType fromId(int id) {
		SeatGradeType type = ID_MAP.get(id);
		if (type == null) {
			throw new BusinessException(SeatGradeErrorCode.INVALID_GRADE_TYPE);
		}
		return type;
	}

	// 조회 이름
	public static SeatGradeType fromDisplayName(String displayName) {
		if (displayName == null || displayName.isBlank()) {
			throw new BusinessException(SeatGradeErrorCode.GRADE_REQUIRED);
		}

		SeatGradeType type = DISPLAY_NAME_MAP.get(displayName.trim());
		if (type == null) {
			throw new BusinessException(SeatGradeErrorCode.INVALID_GRADE_TYPE);
		}
		return type;
	}