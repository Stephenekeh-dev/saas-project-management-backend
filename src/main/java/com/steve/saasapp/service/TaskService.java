package com.steve.saasapp.service;

import com.steve.saasapp.dto.TaskRequestDTO;
import com.steve.saasapp.dto.TaskResponseDTO;
import com.steve.saasapp.model.User;

import java.util.List;

public interface TaskService {
    TaskResponseDTO createTask(Long projectId, TaskRequestDTO dto, User creator);
    List<TaskResponseDTO> getTasksByProject(Long projectId, User user);
    TaskResponseDTO updateTask(Long taskId, TaskRequestDTO dto, User user);
    void deleteTask(Long taskId, User user); // soft delete
}
