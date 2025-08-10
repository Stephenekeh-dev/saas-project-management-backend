package com.steve.saasapp.service.impl;

import com.steve.saasapp.dto.TaskRequestDTO;
import com.steve.saasapp.dto.TaskResponseDTO;
import com.steve.saasapp.exception.ResourceNotFoundException;
import com.steve.saasapp.mapper.TaskMapper;
import com.steve.saasapp.model.Project;
import com.steve.saasapp.model.Task;
import com.steve.saasapp.model.User;
import com.steve.saasapp.model.enums.TaskPriority;
import com.steve.saasapp.model.enums.TaskStatus;
import com.steve.saasapp.repository.ProjectRepository;
import com.steve.saasapp.repository.TaskRepository;
import com.steve.saasapp.repository.UserRepository;
import com.steve.saasapp.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Override
    public TaskResponseDTO createTask(Long projectId, TaskRequestDTO dto, User creator) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        // Tenant ownership check
        if (!project.getTenant().getId().equals(creator.getTenant().getId())) {
            throw new AccessDeniedException("Unauthorized project access");
        }

        Task task = Task.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .status(TaskStatus.valueOf(dto.getStatus().toUpperCase()))
                .priority(TaskPriority.valueOf(dto.getPriority().toUpperCase()))
                .dueDate(dto.getDueDate())
                .project(project)
                .isDeleted(false)
                .build();

        if (dto.getAssignedToUserId() != null) {
            User assignedUser = userRepository.findById(dto.getAssignedToUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned user not found"));
            task.setAssignedTo(assignedUser);
        }

        return TaskMapper.toDTO(taskRepository.save(task));
    }

    @Override
    public List<TaskResponseDTO> getTasksByProject(Long projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!project.getTenant().getId().equals(user.getTenant().getId())) {
            throw new AccessDeniedException("Unauthorized project access");
        }

        return taskRepository.findByProject(project).stream()
                .filter(t -> !t.isDeleted())
                .map(TaskMapper::toDTO)
                .toList();
    }

    @Override
    public TaskResponseDTO updateTask(Long taskId, TaskRequestDTO dto, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (!task.getProject().getTenant().getId().equals(user.getTenant().getId())) {
            throw new AccessDeniedException("Unauthorized task access");
        }

        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(TaskStatus.valueOf(dto.getStatus().toUpperCase()));
        task.setPriority(TaskPriority.valueOf(dto.getPriority().toUpperCase()));
        task.setDueDate(dto.getDueDate());

        if (dto.getAssignedToUserId() != null) {
            User assignedUser = userRepository.findById(dto.getAssignedToUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned user not found"));
            task.setAssignedTo(assignedUser);
        } else {
            task.setAssignedTo(null);
        }

        return TaskMapper.toDTO(taskRepository.save(task));
    }

    @Override
    public void deleteTask(Long taskId, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (!task.getProject().getTenant().getId().equals(user.getTenant().getId())) {
            throw new AccessDeniedException("Unauthorized task deletion");
        }

        task.setDeleted(true);
        taskRepository.save(task);
    }
}
