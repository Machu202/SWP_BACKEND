package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    
    // Tìm toàn bộ công việc do một Mangaka giao
    List<Task> findByMangakaId(Long mangakaId);
    
    // Tìm toàn bộ công việc được phân công cho một Assistant
    List<Task> findByAssistantId(Long assistantId);
    
    // Lấy Task dựa trên Hitbox (Vì quan hệ là 1-1)
    Task findByHitboxId(Long hitboxId);
}