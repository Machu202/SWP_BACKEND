package com.mangastudio.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "System_Parameter_Audit", indexes = {
        @Index(name = "idx_system_parameter_audit_key_changed", columnList = "param_key, changed_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SystemParameterAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "param_key", nullable = false, length = 100)
    private String paramKey;

    @Column(name = "param_type", nullable = false, length = 20)
    private String paramType;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Column(name = "old_value", length = 255)
    private String oldValue;

    @Column(name = "new_value", length = 255)
    private String newValue;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @Column(name = "changed_by_name", nullable = false, length = 255)
    private String changedByName;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}
