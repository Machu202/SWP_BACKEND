package com.mangastudio.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "System_Parameter")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SystemParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "param_key", nullable = false, unique = true, length = 100)
    private String paramKey;

    @Column(name = "param_value", length = 255)
    private String paramValue;

    // Nullable at database level so Hibernate can safely add this column to
    // installations that already contain parameters; the service defaults it to STRING.
    @Column(name = "param_type", length = 20)
    @Builder.Default
    private String paramType = "STRING";

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_by_name", length = 255)
    private String updatedByName;

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
