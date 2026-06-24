package com.mangastudio.backend.entity;

import jakarta.persistence.*;
import lombok.*;

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
}