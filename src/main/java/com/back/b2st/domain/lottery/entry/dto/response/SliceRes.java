package com.back.b2st.domain.lottery.entry.dto.response;

import java.util.List;

import org.springframework.data.domain.Slice;

public record SliceRes<T>(
	List<T> content,
	boolean hasNext
) {
	public static <T> SliceRes<T> from(Slice<T> slice) {
		return new SliceRes<>(slice.getContent(), slice.hasNext());
	}
}
