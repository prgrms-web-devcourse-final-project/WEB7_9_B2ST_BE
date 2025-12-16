package com.back.b2st.domain.bank.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.domain.bank.BankCode;
import com.back.b2st.domain.bank.dto.response.BankRes;
import com.back.b2st.global.common.BaseResponse;

@RestController
@RequestMapping("/api")
public class BankController {

	@GetMapping("/banks")
	public BaseResponse<List<BankRes>> getBankList() {
		List<BankRes> banks = Arrays.stream(BankCode.values())
			.map(BankRes::from)
			.collect(Collectors.toList());

		return BaseResponse.success(banks);
	}
}
