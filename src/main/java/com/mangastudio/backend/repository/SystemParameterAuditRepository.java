package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.SystemParameterAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemParameterAuditRepository extends JpaRepository<SystemParameterAudit, Long> {
}
