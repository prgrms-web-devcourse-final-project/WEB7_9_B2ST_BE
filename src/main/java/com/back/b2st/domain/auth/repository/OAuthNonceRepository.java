package com.back.b2st.domain.auth.repository;

import org.springframework.data.repository.CrudRepository;

import com.back.b2st.domain.auth.entity.OAuthNonce;

public interface OAuthNonceRepository extends CrudRepository<OAuthNonce, String> {
}
