package com.steve.saasapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steve.saasapp.dto.TaskRequestDTO;
import com.steve.saasapp.dto.TaskResponseDTO;
import com.steve.saasapp.exception.ResourceNotFoundException;
import com.steve.saasapp.model.Tenant;
import com.steve.saasapp.model.User;
import com.steve.saasapp.security.CustomUserDetails;
import com.steve.saasapp.security.JwtAuthenticationFilter;
import com.steve.saasapp.security.JwtUtil;
import com.steve.saasapp.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("TaskController")
class TaskControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private TaskService taskService;
    // @MockitoBean private JwtUtil jwtUtil;
    // @MockitoBean private JwtAuthenticationFilter jwtAuthFilter;

    private CustomUserDetails userDetails;
    private TaskRequestDTO requestDTO;
    private TaskResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        Tenant tenant = Tenant.builder().id(1L).name("Acme Corp").build();
        User user = User.builder()
                .id(1L).name("Alice").email("alice@acme.com")
                .tenant(tenant).role(User.Role.ADMIN).build();
        userDetails = new CustomUserDetails(user);

        requestDTO = new TaskRequestDTO();
        requestDTO.setTitle("Design UI");
        requestDTO.setDescription("Create mockups");
        requestDTO.setStatus("TO_DO");
        requestDTO.setPriority("HIGH");
        requestDTO.setDueDate(LocalDate.now().plusDays(7));
        requestDTO.setAssignedToUserId(2L);

        responseDTO = new TaskResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setTitle("Design UI");
        responseDTO.setDescription("Create mockups");
        responseDTO.setStatus("TO_DO");
        responseDTO.setPriority("HIGH");
        responseDTO.setProjectId(1L);
        responseDTO.setAssignedToUserId(2L);
        responseDTO.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("POST /api/projects/{projectId}/tasks")
    class CreateTask {

        @Test
        @DisplayName("returns 201 and created task")
        void createTask_success() throws Exception {
            when(taskService.createTask(eq(1L), any(), any())).thenReturn(responseDTO);

            mockMvc.perform(post("/api/projects/1/tasks")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Design UI"))
                    .andExpect(jsonPath("$.status").value("TO_DO"))
                    .andExpect(jsonPath("$.priority").value("HIGH"));
        }

        @Test
        @DisplayName("returns 404 when project not found")
        void createTask_projectNotFound() throws Exception {
            when(taskService.createTask(eq(99L), any(), any()))
                    .thenThrow(new ResourceNotFoundException("Project not found"));

            mockMvc.perform(post("/api/projects/99/tasks")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 403 when project belongs to different tenant")
        void createTask_forbidden() throws Exception {
            when(taskService.createTask(eq(1L), any(), any()))
                    .thenThrow(new AccessDeniedException("Unauthorized"));

            mockMvc.perform(post("/api/projects/1/tasks")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 403 when no authentication")
        void createTask_unauthenticated() throws Exception {
            mockMvc.perform(post("/api/projects/1/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/projects/{projectId}/tasks")
    class GetTasksByProject {

        @Test
        @DisplayName("returns 200 and list of tasks")
        void getTasksByProject_success() throws Exception {
            when(taskService.getTasksByProject(eq(1L), any())).thenReturn(List.of(responseDTO));

            mockMvc.perform(get("/api/projects/1/tasks").with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].title").value("Design UI"));
        }

        @Test
        @DisplayName("returns 200 and empty list when project has no tasks")
        void getTasksByProject_empty() throws Exception {
            when(taskService.getTasksByProject(eq(1L), any())).thenReturn(List.of());

            mockMvc.perform(get("/api/projects/1/tasks").with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("PUT /api/tasks/{taskId}")
    class UpdateTask {

        @Test
        @DisplayName("returns 200 and updated task")
        void updateTask_success() throws Exception {
            when(taskService.updateTask(eq(1L), any(), any())).thenReturn(responseDTO);

            mockMvc.perform(put("/api/tasks/1")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Design UI"));
        }

        @Test
        @DisplayName("returns 404 when task not found")
        void updateTask_notFound() throws Exception {
            when(taskService.updateTask(eq(99L), any(), any()))
                    .thenThrow(new ResourceNotFoundException("Task not found"));

            mockMvc.perform(put("/api/tasks/99")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/tasks/{id}")
    class DeleteTask {

        @Test
        @DisplayName("returns 200 with success message")
        void deleteTask_success() throws Exception {
            doNothing().when(taskService).deleteTask(eq(1L), any());

            mockMvc.perform(delete("/api/tasks/1").with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Task deleted successfully"));
        }

        @Test
        @DisplayName("returns 404 when task not found")
        void deleteTask_notFound() throws Exception {
            doThrow(new ResourceNotFoundException("Task not found"))
                    .when(taskService).deleteTask(eq(99L), any());

            mockMvc.perform(delete("/api/tasks/99").with(user(userDetails)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 403 when task belongs to different tenant")
        void deleteTask_forbidden() throws Exception {
            doThrow(new AccessDeniedException("Forbidden"))
                    .when(taskService).deleteTask(eq(1L), any());

            mockMvc.perform(delete("/api/tasks/1").with(user(userDetails)))
                    .andExpect(status().isForbidden());
        }
    }
    @Nested
    @DisplayName("RBAC — role enforcement")
    class RbacTaskTests {

        @Test
        @DisplayName("MEMBER cannot create a task — returns 403")
        void createTask_memberForbidden() throws Exception {
            Tenant tenant = Tenant.builder().id(1L).name("Acme Corp").build();
            User member = User.builder()
                    .id(2L).name("Bob").email("bob@acme.com")
                    .tenant(tenant).role(User.Role.MEMBER).build();
            CustomUserDetails memberDetails = new CustomUserDetails(member);

            mockMvc.perform(post("/api/projects/1/tasks")
                            .with(user(memberDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("MEMBER can read tasks — returns 200")
        void getTasks_memberAllowed() throws Exception {
            Tenant tenant = Tenant.builder().id(1L).name("Acme Corp").build();
            User member = User.builder()
                    .id(2L).name("Bob").email("bob@acme.com")
                    .tenant(tenant).role(User.Role.MEMBER).build();
            CustomUserDetails memberDetails = new CustomUserDetails(member);

            when(taskService.getTasksByProject(eq(1L), any())).thenReturn(List.of(responseDTO));

            mockMvc.perform(get("/api/projects/1/tasks")
                            .with(user(memberDetails)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("MANAGER can create a task — returns 201")
        void createTask_managerAllowed() throws Exception {
            Tenant tenant = Tenant.builder().id(1L).name("Acme Corp").build();
            User manager = User.builder()
                    .id(3L).name("Carol").email("carol@acme.com")
                    .tenant(tenant).role(User.Role.MANAGER).build();
            CustomUserDetails managerDetails = new CustomUserDetails(manager);

            when(taskService.createTask(eq(1L), any(), any())).thenReturn(responseDTO);

            mockMvc.perform(post("/api/projects/1/tasks")
                            .with(user(managerDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("MEMBER can update a task — returns 200")
        void updateTask_memberAllowed() throws Exception {
            Tenant tenant = Tenant.builder().id(1L).name("Acme Corp").build();
            User member = User.builder()
                    .id(2L).name("Bob").email("bob@acme.com")
                    .tenant(tenant).role(User.Role.MEMBER).build();
            CustomUserDetails memberDetails = new CustomUserDetails(member);

            when(taskService.updateTask(eq(1L), any(), any())).thenReturn(responseDTO);

            mockMvc.perform(put("/api/tasks/1")
                            .with(user(memberDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isOk());
        }
    }

}