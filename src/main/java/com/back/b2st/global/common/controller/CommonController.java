package com.back.b2st.global.common.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.back.b2st.global.common.BankCode;
import com.back.b2st.global.common.BaseResponse;
import com.back.b2st.global.common.dto.response.BankRes;

@RestController
@RequestMapping("/common")
public class CommonController {

	@GetMapping("/banks")
	public BaseResponse<List<BankRes>> getBankList() {
		List<BankRes> banks = Arrays.stream(BankCode.values())
			.map(BankRes::from)
			.collect(Collectors.toList());

		return BaseResponse.success(banks);
	}
}
