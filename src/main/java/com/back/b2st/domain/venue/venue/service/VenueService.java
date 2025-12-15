package com.back.b2st.domain.venue.venue.service;

import com.back.b2st.domain.venue.venue.dto.response.VenueRes;
import com.back.b2st.domain.venue.venue.repository.VenueRepository;
import com.back.b2st.global.error.code.CommonErrorCode;
import com.back.b2st.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VenueService {

	private final VenueRepository venueRepository;

	public VenueRes getVenue(Long venueId) {
		return venueRepository.findById(venueId)
				.map(VenueRes::from)
				.orElseThrow(() -> new BusinessException(
						CommonErrorCode.NOT_FOUND,
						"venueId=" + venueId
				));
	}
}
