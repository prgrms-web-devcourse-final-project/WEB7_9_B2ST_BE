package com.back.b2st.domain.email.repository;

import org.springframework.data.repository.CrudRepository;

import com.back.b2st.domain.email.entity.EmailVerification;

public interface EmailVerificationRepository extends CrudRepository<EmailVerification, String> {
}
