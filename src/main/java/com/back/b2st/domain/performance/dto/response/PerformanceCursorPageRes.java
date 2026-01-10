package com.back.b2st.domain.performance.dto.response;

import java.util.List;

/**
 * Cursor 기반 페이징 응답 DTO
 */
public record PerformanceCursorPageRes(
	List<PerformanceListRes> content,    // 공연 목록
	Long nextCursor,                     // 다음 페이지를 가져올 cursor (null이면 마지막 페이지)
	boolean hasNext                      // 다음 페이지 존재 여부
) {
	public static PerformanceCursorPageRes of(List<PerformanceListRes> content, int size) {
		boolean hasNext = content.size() > size;
		List<PerformanceListRes> actualContent = hasNext
			? content.subList(0, size)
			: content;

		Long nextCursor = hasNext && !actualContent.isEmpty()
			? actualContent.get(actualContent.size() - 1).performanceId()
			: null;

		return new PerformanceCursorPageRes(actualContent, nextCursor, hasNext);
	}
}

