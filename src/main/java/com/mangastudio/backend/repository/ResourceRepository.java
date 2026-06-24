package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
}