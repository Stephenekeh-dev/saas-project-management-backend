package com.steve.saasapp.service.impl;

import com.steve.saasapp.dto.ProjectRequestDTO;
import com.steve.saasapp.dto.ProjectResponseDTO;
import com.steve.saasapp.exception.ProjectAlreadyExistsException;
import com.steve.saasapp.exception.ResourceNotFoundException;
import com.steve.saasapp.mapper.ProjectMapper;
import com.steve.saasapp.model.Project;
import com.steve.saasapp.model.Tenant;
import com.steve.saasapp.model.User;
import com.steve.saasapp.repository.ProjectRepository;
import com.steve.saasapp.service.ProjectServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectServiceImpl")
class ProjectServiceImplTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMapper projectMapper;

    @InjectMocks private ProjectServiceImpl projectService;

    private Tenant tenant;
    private User user;
    private Project project;
    private ProjectRequestDTO requestDTO;
    private ProjectResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        tenant = Tenant.builder().id(1L).name("Acme Corp").build();

        user = User.builder()
                .id(1L)
                .name("Alice")
                .email("alice@acme.com")
                .tenant(tenant)
                .role(User.Role.ADMIN)
                .build();

        project = Project.builder()
                .id(1L)
                .name("Apollo")
                .description("Test project")
                .status("TODO")
                .tenant(tenant)
                .createdBy(user)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .build();

        requestDTO = new ProjectRequestDTO();
        requestDTO.setName("Apollo");
        requestDTO.setDescription("Test project");
        requestDTO.setStatus("TODO");
        requestDTO.setStartDate(LocalDate.now());
        requestDTO.setEndDate(LocalDate.now().plusMonths(3));

        responseDTO = ProjectResponseDTO.builder()
                .id(1L)
                .name("Apollo")
                .description("Test project")
                .status("TODO")
                .tenantName("Acme Corp")
                .createdBy("Alice")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ─── createProject ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createProject")
    class CreateProject {

        @Test
        @DisplayName("creates and returns project when name is unique for tenant")
        void createProject_success() {
            when(projectRepository.existsByNameAndTenant(requestDTO.getName(), tenant))
                    .thenReturn(false);
            when(projectRepository.save(any(Project.class))).thenReturn(project);
            when(projectMapper.toResponseDTO(project)).thenReturn(responseDTO);

            ProjectResponseDTO result = projectService.createProject(requestDTO, user);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Apollo");
            assertThat(result.getTenantName()).isEqualTo("Acme Corp");
            verify(projectRepository).save(any(Project.class));
        }

        @Test
        @DisplayName("throws ProjectAlreadyExistsException when name is duplicate for tenant")
        void createProject_duplicateName_throwsException() {
            when(projectRepository.existsByNameAndTenant(requestDTO.getName(), tenant))
                    .thenReturn(true);

            assertThatThrownBy(() -> projectService.createProject(requestDTO, user))
                    .isInstanceOf(ProjectAlreadyExistsException.class)
                    .hasMessageContaining("already exists");

            verify(projectRepository, never()).save(any());
        }

        @Test
        @DisplayName("defaults status to TODO when request status is null")
        void createProject_nullStatus_defaultsTodo() {
            requestDTO.setStatus(null);
            when(projectRepository.existsByNameAndTenant(any(), any())).thenReturn(false);
            when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
                Project p = inv.getArgument(0);
                assertThat(p.getStatus()).isEqualTo("TODO");
                return project;
            });
            when(projectMapper.toResponseDTO(any())).thenReturn(responseDTO);

            projectService.createProject(requestDTO, user);

            verify(projectRepository).save(any(Project.class));
        }
    }

    // ─── getProjectsForTenant ─────────────────────────────────────────────────

    @Nested
    @DisplayName("getProjectsForTenant")
    class GetProjectsForTenant {

        @Test
        @DisplayName("returns all non-deleted projects for tenant")
        void getProjectsForTenant_returnsList() {
            when(projectRepository.findByTenantAndIsDeletedFalse(tenant))
                    .thenReturn(List.of(project));
            when(projectMapper.toResponseDTO(project)).thenReturn(responseDTO);

            List<ProjectResponseDTO> result = projectService.getProjectsForTenant(user);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Apollo");
        }

        @Test
        @DisplayName("returns empty list when tenant has no projects")
        void getProjectsForTenant_emptyList() {
            when(projectRepository.findByTenantAndIsDeletedFalse(tenant))
                    .thenReturn(List.of());

            List<ProjectResponseDTO> result = projectService.getProjectsForTenant(user);

            assertThat(result).isEmpty();
        }
    }

    // ─── getProjectById ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getProjectById")
    class GetProjectById {

        @Test
        @DisplayName("returns project when it belongs to user's tenant")
        void getProjectById_success() {
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
            when(projectMapper.toResponseDTO(project)).thenReturn(responseDTO);

            ProjectResponseDTO result = projectService.getProjectById(1L, user);

            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when project does not exist")
        void getProjectById_notFound() {
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.getProjectById(99L, user))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("throws AccessDeniedException when project belongs to different tenant")
        void getProjectById_wrongTenant() {
            Tenant otherTenant = Tenant.builder().id(2L).name("Globex").build();
            project.setTenant(otherTenant);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.getProjectById(1L, user))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // ─── updateProject ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateProject")
    class UpdateProject {

        @Test
        @DisplayName("updates project successfully when user owns it")
        void updateProject_success() {
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
            when(projectRepository.existsByNameAndTenant(any(), any())).thenReturn(false);
            when(projectRepository.save(any())).thenReturn(project);
            when(projectMapper.toResponseDTO(project)).thenReturn(responseDTO);

            ProjectResponseDTO result = projectService.updateProject(1L, requestDTO, user);

            assertThat(result).isNotNull();
            verify(projectRepository).save(project);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when project does not exist")
        void updateProject_notFound() {
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.updateProject(99L, requestDTO, user))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("throws AccessDeniedException when project belongs to different tenant")
        void updateProject_wrongTenant() {
            Tenant otherTenant = Tenant.builder().id(2L).name("Globex").build();
            project.setTenant(otherTenant);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.updateProject(1L, requestDTO, user))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("throws ProjectAlreadyExistsException when new name conflicts with another project")
        void updateProject_nameConflict() {
            requestDTO.setName("Gemini"); // different from current "Apollo"
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
            when(projectRepository.existsByNameAndTenant("Gemini", tenant)).thenReturn(true);

            assertThatThrownBy(() -> projectService.updateProject(1L, requestDTO, user))
                    .isInstanceOf(ProjectAlreadyExistsException.class);
        }

        @Test
        @DisplayName("does not check name conflict when name is unchanged")
        void updateProject_sameNameNoConflictCheck() {
            requestDTO.setName("Apollo"); // same as existing project name
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
            when(projectRepository.save(any())).thenReturn(project);
            when(projectMapper.toResponseDTO(project)).thenReturn(responseDTO);

            projectService.updateProject(1L, requestDTO, user);

            verify(projectRepository, never()).existsByNameAndTenant(any(), any());
        }
    }

    // ─── deleteProject ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteProject")
    class DeleteProject {

        @Test
        @DisplayName("soft-deletes project when user owns it")
        void deleteProject_success() {
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

            projectService.deleteProject(1L, user);

            assertThat(project.isDeleted()).isTrue();
            verify(projectRepository).save(project);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when project does not exist")
        void deleteProject_notFound() {
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.deleteProject(99L, user))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("throws AccessDeniedException when project belongs to different tenant")
        void deleteProject_wrongTenant() {
            Tenant otherTenant = Tenant.builder().id(2L).name("Globex").build();
            project.setTenant(otherTenant);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.deleteProject(1L, user))
                    .isInstanceOf(AccessDeniedException.class);

            verify(projectRepository, never()).save(any());
        }
    }

    // ─── getProjectsForTenantPaginated ────────────────────────────────────────

    @Nested
    @DisplayName("getProjectsForTenantPaginated")
    class GetProjectsForTenantPaginated {

        @Test
        @DisplayName("returns paginated results for tenant")
        void paginated_returnsPage() {
            Page<Project> projectPage = new PageImpl<>(List.of(project));
            when(projectRepository
                    .findByTenantIdAndNameContainingIgnoreCaseAndStatusContainingIgnoreCaseAndIsDeletedFalse(
                            eq(1L), anyString(), anyString(), any(Pageable.class)))
                    .thenReturn(projectPage);
            when(projectMapper.toResponseDTO(project)).thenReturn(responseDTO);

            Page<ProjectResponseDTO> result =
                    projectService.getProjectsForTenantPaginated(user, 0, 10, null, null);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Apollo");
        }

        @Test
        @DisplayName("passes empty string for null name and status filters")
        void paginated_nullFilters_usesEmptyString() {
            Page<Project> emptyPage = new PageImpl<>(List.of());
            when(projectRepository
                    .findByTenantIdAndNameContainingIgnoreCaseAndStatusContainingIgnoreCaseAndIsDeletedFalse(
                            eq(1L), eq(""), eq(""), any(Pageable.class)))
                    .thenReturn(emptyPage);

            Page<ProjectResponseDTO> result =
                    projectService.getProjectsForTenantPaginated(user, 0, 10, null, null);

            assertThat(result.getContent()).isEmpty();
        }
    }
}
