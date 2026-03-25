package com.steve.saasapp.service.impl;

import com.steve.saasapp.dto.TaskRequestDTO;
import com.steve.saasapp.dto.TaskResponseDTO;
import com.steve.saasapp.exception.ResourceNotFoundException;
import com.steve.saasapp.model.Project;
import com.steve.saasapp.model.Task;
import com.steve.saasapp.model.Tenant;
import com.steve.saasapp.model.User;
import com.steve.saasapp.model.enums.TaskPriority;
import com.steve.saasapp.model.enums.TaskStatus;
import com.steve.saasapp.repository.ProjectRepository;
import com.steve.saasapp.repository.TaskRepository;
import com.steve.saasapp.repository.UserRepository;
import com.steve.saasapp.service.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskServiceImpl")
class TaskServiceImplTest {

    @Mock private TaskRepository taskRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private TaskServiceImpl taskService;

    private Tenant tenant;
    private User user;
    private User assignee;
    private Project project;
    private Task task;
    private TaskRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        tenant = Tenant.builder().id(1L).name("Acme Corp").build();

        user = User.builder()
                .id(1L).name("Alice").email("alice@acme.com")
                .tenant(tenant).role(User.Role.ADMIN).build();

        assignee = User.builder()
                .id(2L).name("Bob").email("bob@acme.com")
                .tenant(tenant).role(User.Role.MEMBER).build();

        project = Project.builder()
                .id(1L).name("Apollo").tenant(tenant).createdBy(user)
                .isDeleted(false).build();

        task = Task.builder()
                .id(1L).title("Design UI").description("Create mockups")
                .status(TaskStatus.TO_DO).priority(TaskPriority.HIGH)
                .dueDate(LocalDate.now().plusDays(7))
                .project(project).assignedTo(assignee)
                .isDeleted(false).createdAt(LocalDateTime.now()).build();

        requestDTO = new TaskRequestDTO();
        requestDTO.setTitle("Design UI");
        requestDTO.setDescription("Create mockups");
        requestDTO.setStatus("TO_DO");
        requestDTO.setPriority("HIGH");
        requestDTO.setDueDate(LocalDate.now().plusDays(7));
        requestDTO.setAssignedToUserId(2L);
    }

    // ─── createTask ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createTask")
    class CreateTask {

        @Test
        @DisplayName("creates task successfully for own tenant's project")
        void createTask_success() {
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
            when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
            when(taskRepository.save(any(Task.class))).thenReturn(task);

            TaskResponseDTO result = taskService.createTask(1L, requestDTO, user);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Design UI");
            assertThat(result.getStatus()).isEqualTo("TO_DO");
            assertThat(result.getPriority()).isEqualTo("HIGH");
            verify(taskRepository).save(any(Task.class));
        }

        @Test
        @DisplayName("creates task without assignee when assignedToUserId is null")
        void createTask_noAssignee() {
            requestDTO.setAssignedToUserId(null);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
            when(taskRepository.save(any(Task.class))).thenReturn(task);

            taskService.createTask(1L, requestDTO, user);

            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when project does not exist")
        void createTask_projectNotFound() {
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.createTask(99L, requestDTO, user))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Project not found");
        }

        @Test
        @DisplayName("throws AccessDeniedException when project belongs to different tenant")
        void createTask_wrongTenant() {
            Tenant otherTenant = Tenant.builder().id(2L).name("Globex").build();
            project.setTenant(otherTenant);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> taskService.createTask(1L, requestDTO, user))
                    .isInstanceOf(AccessDeniedException.class);

            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when assigned user does not exist")
        void createTask_assigneeNotFound() {
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
            when(userRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.createTask(1L, requestDTO, user))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Assigned user not found");
        }

        @Test
        @DisplayName("throws IllegalArgumentException for invalid status value")
        void createTask_invalidStatus() {
            requestDTO.setStatus("INVALID_STATUS");
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> taskService.createTask(1L, requestDTO, user))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─── getTasksByProject ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTasksByProject")
    class GetTasksByProject {

        @Test
        @DisplayName("returns non-deleted tasks for project")
        void getTasksByProject_success() {
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
            when(taskRepository.findByProject(project)).thenReturn(List.of(task));

            List<TaskResponseDTO> result = taskService.getTasksByProject(1L, user);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Design UI");
        }

        @Test
        @DisplayName("filters out soft-deleted tasks")
        void getTasksByProject_excludesDeleted() {
            Task deletedTask = Task.builder()
                    .id(2L).title("Old task").project(project)
                    .status(TaskStatus.DONE).priority(TaskPriority.LOW)
                    .isDeleted(true).build();

            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
            when(taskRepository.findByProject(project)).thenReturn(List.of(task, deletedTask));

            List<TaskResponseDTO> result = taskService.getTasksByProject(1L, user);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Design UI");
        }

        @Test
        @DisplayName("throws AccessDeniedException for different tenant's project")
        void getTasksByProject_wrongTenant() {
            Tenant otherTenant = Tenant.builder().id(2L).name("Globex").build();
            project.setTenant(otherTenant);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> taskService.getTasksByProject(1L, user))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // ─── updateTask ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateTask")
    class UpdateTask {

        @Test
        @DisplayName("updates task fields successfully")
        void updateTask_success() {
            requestDTO.setTitle("Updated Title");
            requestDTO.setStatus("IN_PROGRESS");
            requestDTO.setPriority("MEDIUM");

            when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
            when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
            when(taskRepository.save(any(Task.class))).thenReturn(task);

            TaskResponseDTO result = taskService.updateTask(1L, requestDTO, user);

            assertThat(result).isNotNull();
            verify(taskRepository).save(task);
        }

        @Test
        @DisplayName("clears assignee when assignedToUserId is null")
        void updateTask_clearsAssignee() {
            requestDTO.setAssignedToUserId(null);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
            when(taskRepository.save(any(Task.class))).thenReturn(task);

            taskService.updateTask(1L, requestDTO, user);

            assertThat(task.getAssignedTo()).isNull();
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when task does not exist")
        void updateTask_notFound() {
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateTask(99L, requestDTO, user))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("throws AccessDeniedException when task belongs to different tenant")
        void updateTask_wrongTenant() {
            Tenant otherTenant = Tenant.builder().id(2L).name("Globex").build();
            project.setTenant(otherTenant);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> taskService.updateTask(1L, requestDTO, user))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // ─── deleteTask ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteTask")
    class DeleteTask {

        @Test
        @DisplayName("soft-deletes task successfully")
        void deleteTask_success() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

            taskService.deleteTask(1L, user);

            assertThat(task.isDeleted()).isTrue();
            verify(taskRepository).save(task);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when task does not exist")
        void deleteTask_notFound() {
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.deleteTask(99L, user))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("throws AccessDeniedException when task belongs to different tenant")
        void deleteTask_wrongTenant() {
            Tenant otherTenant = Tenant.builder().id(2L).name("Globex").build();
            project.setTenant(otherTenant);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> taskService.deleteTask(1L, user))
                    .isInstanceOf(AccessDeniedException.class);

            verify(taskRepository, never()).save(any());
        }
    }
}