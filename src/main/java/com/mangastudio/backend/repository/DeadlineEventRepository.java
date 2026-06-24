package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.DeadlineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadlineEventRepository extends JpaRepository<DeadlineEvent, Long> {
}