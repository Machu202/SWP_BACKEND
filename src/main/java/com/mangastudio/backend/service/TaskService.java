package com.mangastudio.backend.service;

import com.mangastudio.backend.entity.Task;
import java.util.List;

public interface TaskService {
    List<Task> getTasksByMangaka(Long mangakaId);
    List<Task> getTasksByAssistant(Long assistantId);
    
    Task assignAssistantToTask(Long taskId, Long mangakaId, Long assistantId);
    Task updateTaskStatus(Long taskId, Long userId, String newStatus);
    Task submitTaskWork(Long taskId, Long assistantId, String imageUrl);
}