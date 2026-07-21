package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    
    // Finds every task assigned by a Mangaka, including Series/Page/Assistant display metadata.
    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.assistant assistant " +
           "LEFT JOIN FETCH t.mangaka mangaka " +
           "LEFT JOIN FETCH t.hitbox hitbox " +
           "LEFT JOIN FETCH hitbox.page page " +
           "LEFT JOIN FETCH page.chapter chapter " +
           "LEFT JOIN FETCH chapter.mangaSeries series " +
           "WHERE t.mangaka.id = :mangakaId ORDER BY t.createdAt DESC")
    List<Task> findByMangakaId(@Param("mangakaId") Long mangakaId);
    
    // Finds every task assigned to an Assistant, including Series/Page/Assistant display metadata.
    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.assistant assistant " +
           "LEFT JOIN FETCH t.mangaka mangaka " +
           "LEFT JOIN FETCH t.hitbox hitbox " +
           "LEFT JOIN FETCH hitbox.page page " +
           "LEFT JOIN FETCH page.chapter chapter " +
           "LEFT JOIN FETCH chapter.mangaSeries series " +
           "WHERE t.assistant.id = :assistantId ORDER BY t.createdAt DESC")
    List<Task> findByAssistantId(@Param("assistantId") Long assistantId);

    // Tantou sees a read-only Kanban of tasks from only the manga series assigned to them.
    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.assistant assistant " +
           "LEFT JOIN FETCH t.mangaka mangaka " +
           "LEFT JOIN FETCH t.hitbox hitbox " +
           "LEFT JOIN FETCH hitbox.page page " +
           "LEFT JOIN FETCH page.chapter chapter " +
           "LEFT JOIN FETCH chapter.mangaSeries series " +
           "WHERE series.tantou.id = :tantouId ORDER BY t.createdAt DESC")
    List<Task> findByAssignedTantouId(@Param("tantouId") Long tantouId);
    
    // Finds a task by its one-to-one hitbox relationship.
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

    boolean existsByMangaka_IdAndAssistant_Id(Long mangakaId, Long assistantId);

    @Query("SELECT COUNT(t) FROM Task t " +
           "JOIN t.hitbox hitbox JOIN hitbox.page page JOIN page.chapter chapter JOIN chapter.mangaSeries series " +
           "WHERE series.id = :seriesId AND t.id <= :taskId")
    long countSeriesTasksUpToId(@Param("seriesId") Long seriesId, @Param("taskId") Long taskId);
}
