package com.steve.saasapp.mapper;

import com.steve.saasapp.dto.TaskResponseDTO;
import com.steve.saasapp.model.Task;

public class TaskMapper {
    public static TaskResponseDTO toDTO(Task task) {
        TaskResponseDTO dto = new TaskResponseDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setStatus(task.getStatus() != null ? task.getStatus().name() : null);
        dto.setPriority(task.getPriority() != null ? task.getPriority().name() : null);
        dto.setDueDate(task.getDueDate());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setProjectId(task.getProject().getId());
        dto.setAssignedToUserId(task.getAssignedTo() != null ? task.getAssignedTo().getId() : null);
        return dto;
    }
}
