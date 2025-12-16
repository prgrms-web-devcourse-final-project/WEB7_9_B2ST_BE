package com.back.b2st.domain.ticket.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.ticket.dto.response.TicketRes;
import com.back.b2st.domain.ticket.service.TicketService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "🎫 Ticket", description = "티켓 API")
public class TicketController {

	private final TicketService ticketService;

	@GetMapping("/my")
	@Operation(summary = "내 티켓 목록 조회", description = "본인이 소유한 티켓 목록을 조회합니다")
	public ResponseEntity<BaseResponse<List<TicketRes>>> getMyTickets(
		@CurrentUser UserPrincipal userPrincipal
	) {
		List<TicketRes> response = ticketService.getMyTickets(userPrincipal.getId());
		return ResponseEntity.ok(BaseResponse.success(response));
	}
}
