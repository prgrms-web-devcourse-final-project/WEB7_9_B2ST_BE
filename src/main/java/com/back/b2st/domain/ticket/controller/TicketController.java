package com.back.b2st.domain.ticket.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.ticket.api.TicketApi;
import com.back.b2st.domain.ticket.dto.response.TicketRes;
import com.back.b2st.domain.ticket.service.TicketService;
import com.back.b2st.global.annotation.CurrentUser;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController implements TicketApi {

	private final TicketService ticketService;

	@Override
	@GetMapping("/my")
	public ResponseEntity<BaseResponse<List<TicketRes>>> getMyTickets(
		@CurrentUser UserPrincipal userPrincipal
	) {
		List<TicketRes> response = ticketService.getMyTickets(userPrincipal.getId());
		return ResponseEntity.ok(BaseResponse.success(response));
	}
}
