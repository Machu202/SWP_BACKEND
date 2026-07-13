package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    
    // Tìm toàn bộ công việc do một Mangaka giao, kèm metadata để frontend hiển thị Series/Page/Assistant.
    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.assistant assistant " +
           "LEFT JOIN FETCH t.mangaka mangaka " +
           "LEFT JOIN FETCH t.hitbox hitbox " +
           "LEFT JOIN FETCH hitbox.page page " +
           "LEFT JOIN FETCH page.chapter chapter " +
           "LEFT JOIN FETCH chapter.mangaSeries series " +
           "WHERE t.mangaka.id = :mangakaId ORDER BY t.createdAt DESC")
    List<Task> findByMangakaId(@Param("mangakaId") Long mangakaId);
    
    // Tìm toàn bộ công việc được phân công cho một Assistant, kèm metadata để frontend hiển thị Series/Page/Assistant.
    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.assistant assistant " +
           "LEFT JOIN FETCH t.mangaka mangaka " +
           "LEFT JOIN FETCH t.hitbox hitbox " +
           "LEFT JOIN FETCH hitbox.page page " +
           "LEFT JOIN FETCH page.chapter chapter " +
           "LEFT JOIN FETCH chapter.mangaSeries series " +
           "WHERE t.assistant.id = :assistantId ORDER BY t.createdAt DESC")
    List<Task> findByAssistantId(@Param("assistantId") Long assistantId);
    
    // Lấy Task dựa trên Hitbox (Vì quan hệ là 1-1)
    Task findByHitboxId(Long hitboxId);

    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.assistant assistant " +
           "LEFT JOIN FETCH t.mangaka mangaka " +
           "LEFT JOIN FETCH t.hitbox hitbox " +
           "LEFT JOIN FETCH hitbox.page page " +
           "LEFT JOIN FETCH page.chapter chapter " +
           "LEFT JOIN FETCH chapter.mangaSeries series " +
           "WHERE series.id = :seriesId ORDER BY t.createdAt DESC")
    List<Task> findAllTasksBySeriesId(@Param("seriesId") Long seriesId);
    boolean existsByHitbox_Page_Chapter_IdAndStatusIgnoreCase(Long chapterId, String status);
    long countByHitbox_Page_Chapter_Id(Long chapterId);
    long countByHitbox_Page_Chapter_IdAndStatusIgnoreCase(Long chapterId, String status);

    @Query("SELECT COUNT(t) FROM Task t " +
           "JOIN t.hitbox hitbox JOIN hitbox.page page JOIN page.chapter chapter JOIN chapter.mangaSeries series " +
           "WHERE series.id = :seriesId AND t.id <= :taskId")
    long countSeriesTasksUpToId(@Param("seriesId") Long seriesId, @Param("taskId") Long taskId);
}
