package com.back.b2st.domain.ticket.api;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.back.b2st.domain.ticket.dto.response.TicketRes;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "🎫 Ticket", description = "티켓 API")
public interface TicketApi {

	@Operation(summary = "내 티켓 목록 조회", description = "본인이 소유한 티켓 목록을 조회합니다")
	ResponseEntity<BaseResponse<List<TicketRes>>> getMyTickets(
		@Parameter(hidden = true) UserPrincipal userPrincipal
	);
}
