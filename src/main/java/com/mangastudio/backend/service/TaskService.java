package com.mangastudio.backend.service;

import com.mangastudio.backend.entity.Task;
import java.util.List;

public interface TaskService {
    List<Task> getTasksByMangaka(Long mangakaId);
    List<Task> getTasksByAssistant(Long assistantId);
    List<Task> getTasksByTantou(Long tantouId);
    
    Task assignAssistantToTask(Long taskId, Long mangakaId, Long assistantId);
    Task updateTaskStatus(Long taskId, Long userId, String newStatus);
    Task startTask(Long taskId, Long assistantId);
    Task reviewTask(Long taskId, Long mangakaId, boolean approved);
    Task submitTaskWork(Long taskId, Long assistantId, String imageUrl);

    List<Task> getTasksBySeries(Long seriesId);
    long getSeriesTaskNumber(Task task);
}