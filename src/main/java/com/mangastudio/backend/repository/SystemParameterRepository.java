package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.SystemParameter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemParameterRepository extends JpaRepository<SystemParameter, Long> {
    
    Optional<SystemParameter> findByParamKey(String paramKey);
}