package com.back.b2st.domain.auth.repository;

import org.springframework.data.repository.CrudRepository;

import com.back.b2st.domain.auth.entity.RefreshToken;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
}
