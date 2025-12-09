package com.back.b2st.global.jpa.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@SuppressWarnings("checkstyle:RegexpMultiline")
@MappedSuperclass
@Getter
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

	@Column(name = "create_at", nullable = false, updatable = false)
	@CreatedDate
	private LocalDateTime createdAt;

	@Column(name = "create_by")
	@CreatedBy
	private long createdBy;

	@Column(name = "modified_at")
	@LastModifiedDate
	private LocalDateTime modifiedAt;

	@Column(name = "modified_by")
	@LastModifiedBy
	private long modifiedBy;

}
