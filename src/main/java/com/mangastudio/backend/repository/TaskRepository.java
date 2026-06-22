package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByPageId(Long pageId);
    List<Task> findByAssistantId(Long assistantId);
    long countByPageIdAndStatusNot(Long pageId, Task.TaskStatus status);
}