package com.steve.saasapp.service.impl;

import com.steve.saasapp.dto.ProjectRequestDTO;
import com.steve.saasapp.dto.ProjectResponseDTO;
import com.steve.saasapp.exception.ProjectAlreadyExistsException;
import com.steve.saasapp.exception.ResourceNotFoundException;
import com.steve.saasapp.model.Project;
import com.steve.saasapp.model.User;
import com.steve.saasapp.repository.ProjectRepository;
import com.steve.saasapp.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import com.steve.saasapp.mapper.ProjectMapper;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    @Override
    public ProjectResponseDTO createProject(ProjectRequestDTO dto, User creator) {
        // 🔐 Check if a project with the same name exists for the current tenant
        if (projectRepository.existsByNameAndTenant(dto.getName(), creator.getTenant())) {
            throw new ProjectAlreadyExistsException("Project with the same name already exists for this tenant.");
        }

        Project project = Project.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .tenant(creator.getTenant())
                .status(dto.getStatus() != null ? dto.getStatus() : "TODO")
                .createdBy(creator)
                .build();

        Project saved = projectRepository.save(project);
        return mapToDTO(saved);
    }
    @Override
    public List<ProjectResponseDTO> getProjectsForTenant(User user) {
        return projectRepository.findByTenantAndIsDeletedFalse(user.getTenant())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    private ProjectResponseDTO mapToDTO(Project project) {
        return ProjectResponseDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .tenantName(project.getTenant().getName())
                .createdBy(project.getCreatedBy().getName())
                .createdAt(project.getCreatedAt())
                .build();
    }
    @Override
    public ProjectResponseDTO updateProject(Long projectId, ProjectRequestDTO dto, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + projectId));

        // Ensure tenant ownership
        if (!project.getTenant().getId().equals(user.getTenant().getId())) {
            throw new AccessDeniedException("You don't have permission to update this project.");
        }

        // Check for name conflict if name is changing
        if (!project.getName().equals(dto.getName()) &&
                projectRepository.existsByNameAndTenant(dto.getName(), user.getTenant())) {
            throw new ProjectAlreadyExistsException("Another project with the same name exists.");
        }

        // Apply updates
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setStartDate(dto.getStartDate());
        project.setEndDate(dto.getEndDate());
        project.setStatus(dto.getStatus());
        Project saved = projectRepository.save(project);
        return mapToDTO(saved);
    }
    @Override
    public void deleteProject(Long id, User user) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        // Ownership check
        if (!project.getTenant().getId().equals(user.getTenant().getId())) {
            throw new AccessDeniedException("You don't have permission to delete this project.");
        }

        project.setDeleted(true); // Soft delete
        projectRepository.save(project);
    }

    @Override
    public ProjectResponseDTO getProjectById(Long projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + projectId));

        // Ensure that the project belongs to the requesting user's tenant
        if (!project.getTenant().getId().equals(user.getTenant().getId())) {
            throw new AccessDeniedException("Access denied: You don't own this project.");
        }

        return projectMapper.toResponseDTO(project); // Assuming you have a mapper
    }
    @Override
    public Page<ProjectResponseDTO> getProjectsForTenantPaginated(
            User user,
            int page,
            int size,
            String name,
            String status
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Project> projectPage = projectRepository
                .findByTenantIdAndNameContainingIgnoreCaseAndStatusContainingIgnoreCaseAndIsDeletedFalse(
                        user.getTenant().getId(),
                        name != null ? name : "",
                        status != null ? status : "",
                        pageable
                );

        return projectPage.map(projectMapper::toResponseDTO);
    }



}