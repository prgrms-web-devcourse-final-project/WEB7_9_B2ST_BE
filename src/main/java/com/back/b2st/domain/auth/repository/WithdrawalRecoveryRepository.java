package com.back.b2st.domain.auth.repository;

import org.springframework.data.repository.CrudRepository;

import com.back.b2st.domain.auth.entity.WithdrawalRecoveryToken;

public interface WithdrawalRecoveryRepository extends CrudRepository<WithdrawalRecoveryToken, String> {
}
