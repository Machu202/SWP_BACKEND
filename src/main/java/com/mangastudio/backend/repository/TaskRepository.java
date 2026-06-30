package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    
    // Tìm toàn bộ công việc do một Mangaka giao
    List<Task> findByMangakaId(Long mangakaId);
    
    // Tìm toàn bộ công việc được phân công cho một Assistant
    List<Task> findByAssistantId(Long assistantId);
    
    // Lấy Task dựa trên Hitbox (Vì quan hệ là 1-1)
    Task findByHitboxId(Long hitboxId);

    @Query("SELECT t FROM Task t WHERE t.hitbox.page.chapter.mangaSeries.id = :seriesId ORDER BY t.createdAt DESC")
    List<Task> findAllTasksBySeriesId(@Param("seriesId") Long seriesId);
}