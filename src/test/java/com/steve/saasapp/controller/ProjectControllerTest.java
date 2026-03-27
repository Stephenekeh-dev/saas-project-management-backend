package com.steve.saasapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steve.saasapp.dto.ProjectRequestDTO;
import com.steve.saasapp.dto.ProjectResponseDTO;
import com.steve.saasapp.exception.ProjectAlreadyExistsException;
import com.steve.saasapp.exception.ResourceNotFoundException;
import com.steve.saasapp.model.Tenant;
import com.steve.saasapp.model.User;
import com.steve.saasapp.security.CustomUserDetails;
import com.steve.saasapp.security.JwtAuthenticationFilter;
import com.steve.saasapp.security.JwtUtil;
import com.steve.saasapp.service.ProjectService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
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
@DisplayName("ProjectController")
class ProjectControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ProjectService projectService;

    private CustomUserDetails userDetails;
    private ProjectRequestDTO requestDTO;
    private ProjectResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        Tenant tenant = Tenant.builder().id(1L).name("Acme Corp").build();
        User user = User.builder()
                .id(1L).name("Alice").email("alice@acme.com")
                .tenant(tenant).role(User.Role.ADMIN).build();
        userDetails = new CustomUserDetails(user);

        requestDTO = new ProjectRequestDTO();
        requestDTO.setName("Apollo");
        requestDTO.setDescription("Test project");
        requestDTO.setStatus("TODO");
        requestDTO.setStartDate(LocalDate.now());
        requestDTO.setEndDate(LocalDate.now().plusMonths(3));

        responseDTO = ProjectResponseDTO.builder()
                .id(1L).name("Apollo").description("Test project")
                .status("TODO").tenantName("Acme Corp").createdBy("Alice")
                .createdAt(LocalDateTime.now()).build();
    }

    @Nested
    @DisplayName("POST /api/projects")
    class CreateProject {

        @Test
        @DisplayName("returns 200 and created project")
        void createProject_success() throws Exception {
            when(projectService.createProject(any(), any())).thenReturn(responseDTO);

            mockMvc.perform(post("/api/projects")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Apollo"))
                    .andExpect(jsonPath("$.tenantName").value("Acme Corp"));
        }

        @Test
        @DisplayName("returns 409 when project name already exists")
        void createProject_conflict() throws Exception {
            when(projectService.createProject(any(), any()))
                    .thenThrow(new ProjectAlreadyExistsException("Project already exists"));

            mockMvc.perform(post("/api/projects")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 401 when no authentication provided")
        void createProject_unauthenticated() throws Exception {
            mockMvc.perform(post("/api/projects")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/projects/all")
    class GetAllProjects {

        @Test
        @DisplayName("returns 200 and list of projects")
        void getAllProjects_success() throws Exception {
            when(projectService.getProjectsForTenant(any())).thenReturn(List.of(responseDTO));

            mockMvc.perform(get("/api/projects/all").with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("Apollo"));
        }

        @Test
        @DisplayName("returns 200 and empty list when tenant has no projects")
        void getAllProjects_empty() throws Exception {
            when(projectService.getProjectsForTenant(any())).thenReturn(List.of());

            mockMvc.perform(get("/api/projects/all").with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/projects (paginated)")
    class GetProjectsPaginated {

        @Test
        @DisplayName("returns 200 with paginated content")
        void getProjectsPaginated_success() throws Exception {
            Page<ProjectResponseDTO> page = new PageImpl<>(List.of(responseDTO));
            when(projectService.getProjectsForTenantPaginated(any(), eq(0), eq(10), any(), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/projects")
                            .with(user(userDetails))
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Apollo"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/projects/{id}")
    class GetProjectById {

        @Test
        @DisplayName("returns 200 and project when found")
        void getProjectById_success() throws Exception {
            when(projectService.getProjectById(eq(1L), any())).thenReturn(responseDTO);

            mockMvc.perform(get("/api/projects/1").with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Apollo"));
        }

        @Test
        @DisplayName("returns 404 when project not found")
        void getProjectById_notFound() throws Exception {
            when(projectService.getProjectById(eq(99L), any()))
                    .thenThrow(new ResourceNotFoundException("Project not found"));

            mockMvc.perform(get("/api/projects/99").with(user(userDetails)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 403 when project belongs to different tenant")
        void getProjectById_forbidden() throws Exception {
            when(projectService.getProjectById(eq(1L), any()))
                    .thenThrow(new AccessDeniedException("Access denied"));

            mockMvc.perform(get("/api/projects/1").with(user(userDetails)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/projects/{id}")
    class UpdateProject {

        @Test
        @DisplayName("returns 200 and updated project")
        void updateProject_success() throws Exception {
            when(projectService.updateProject(eq(1L), any(), any())).thenReturn(responseDTO);

            mockMvc.perform(put("/api/projects/1")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Apollo"));
        }

        @Test
        @DisplayName("returns 404 when project not found")
        void updateProject_notFound() throws Exception {
            when(projectService.updateProject(eq(99L), any(), any()))
                    .thenThrow(new ResourceNotFoundException("Not found"));

            mockMvc.perform(put("/api/projects/99")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/projects/{id}")
    class DeleteProject {

        @Test
        @DisplayName("returns 200 with success message")
        void deleteProject_success() throws Exception {
            doNothing().when(projectService).deleteProject(eq(1L), any());

            mockMvc.perform(delete("/api/projects/1").with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Project deleted successfully."));
        }

        @Test
        @DisplayName("returns 404 when project not found")
        void deleteProject_notFound() throws Exception {
            doThrow(new ResourceNotFoundException("Not found"))
                    .when(projectService).deleteProject(eq(99L), any());

            mockMvc.perform(delete("/api/projects/99").with(user(userDetails)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 403 when project belongs to different tenant")
        void deleteProject_forbidden() throws Exception {
            doThrow(new AccessDeniedException("Forbidden"))
                    .when(projectService).deleteProject(eq(1L), any());

            mockMvc.perform(delete("/api/projects/1").with(user(userDetails)))
                    .andExpect(status().isForbidden());
        }
    }

}