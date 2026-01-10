package com.back.b2st.domain.lottery.draw.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.lottery.draw.service.DrawService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/draw")
@RequiredArgsConstructor
public class DrawController {

	private final DrawService drawService;

	// test
	@PostMapping("/executedraws")
	public void executeDraws() {
		drawService.executeDraws();
	}

	// test
	@PostMapping("/executeAllocation")
	public void executeAllocation() {
		drawService.executeAllocation();
	}

	// test
	@PostMapping("/executecancelUnpaid")
	public void executecancelUnpaid() {
		drawService.executecancelUnpaid();
	}
}
