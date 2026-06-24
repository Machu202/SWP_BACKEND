package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.PublishingSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublishingScheduleRepository extends JpaRepository<PublishingSchedule, Long> {
}