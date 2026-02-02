package com.back.b2st.domain.performance.entity;

public enum PerformanceStatus {
	ACTIVE, // 활성(노출 가능). 예매 가능 여부는 bookingOpenAt 기준
	ENDED   // 종료(노출/예매 불가)
}
