package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.entity.Task;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.TaskRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Override
    public List<Task> getTasksByMangaka(Long mangakaId) {
        return taskRepository.findByMangakaId(mangakaId);
    }

    @Override
    public List<Task> getTasksByAssistant(Long assistantId) {
        return taskRepository.findByAssistantId(assistantId);
    }

    @Override
    @Transactional
    public Task assignAssistantToTask(Long taskId, Long mangakaId, Long assistantId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Error: Task not found"));

        if (!task.getMangaka().getId().equals(mangakaId)) {
            throw new RuntimeException("Error: Only the Mangaka who created this task can assign an assistant.");
        }

        User assistant = userRepository.findById(assistantId)
                .orElseThrow(() -> new RuntimeException("Error: Assistant not found"));

        task.setAssistant(assistant);
        return taskRepository.save(task);
    }

    // [FE-39] State Machine cho Kanban
    @Override
    @Transactional
    public Task updateTaskStatus(Long taskId, Long userId, String newStatusStr) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Error: Task not found"));

        String currentStatus = task.getStatus() != null ? task.getStatus().toUpperCase() : "TODO";
        String newStatus = newStatusStr.toUpperCase();
        boolean isValidTransition = false;

        switch (currentStatus) {
            case "TODO":
                if (newStatus.equals("DOING")) isValidTransition = true;
                break;
            case "DOING":
                if (newStatus.equals("REVIEWING") || newStatus.equals("TODO")) isValidTransition = true;
                break;
            case "REVIEWING":
                if (newStatus.equals("APPROVED") || newStatus.equals("DOING")) isValidTransition = true;
                break;
            case "APPROVED":
                // Đã chốt duyệt thì không cho lùi lại nữa
                isValidTransition = false;
                break;
        }

        if (!isValidTransition) {
            throw new RuntimeException("Error: Invalid task status transition from " + currentStatus + " to " + newStatus);
        }

        task.setStatus(newStatus);
        return taskRepository.save(task);
    }

    @Override
    @Transactional
    public Task submitTaskWork(Long taskId, Long assistantId, String imageUrl) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Error: Task not found"));

        // Xác minh đúng Assistant đang nhận việc này mới được nộp
        if (task.getAssistant() == null || !task.getAssistant().getId().equals(assistantId)) {
            throw new RuntimeException("Error: You are not assigned to this task.");
        }

        task.setSubmittedImageUrl(imageUrl);
        task.setStatus("REVIEWING"); // Nộp xong thì tự động đẩy cột Kanban sang cột duyệt
        
        return taskRepository.save(task);
    }
}