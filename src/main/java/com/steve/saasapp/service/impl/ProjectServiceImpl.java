package com.steve.saasapp.service.impl;

import com.steve.saasapp.dto.ProjectRequestDTO;
import com.steve.saasapp.dto.ProjectResponseDTO;
import com.steve.saasapp.exception.ProjectAlreadyExistsException;
import com.steve.saasapp.exception.ResourceNotFoundException;
import com.steve.saasapp.mapper.ProjectMapper;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    @Override
    public ProjectResponseDTO createProject(ProjectRequestDTO dto, User creator) {
        if (projectRepository.existsByNameAndTenant(dto.getName(), creator.getTenant())) {
            throw new ProjectAlreadyExistsException(
                    "Project with the same name already exists for this tenant.");
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

        return projectMapper.toResponseDTO(projectRepository.save(project));
    }

    @Override
    public List<ProjectResponseDTO> getProjectsForTenant(User user) {
        return projectRepository.findByTenantAndIsDeletedFalse(user.getTenant())
                .stream()
                .map(projectMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ProjectResponseDTO updateProject(Long projectId, ProjectRequestDTO dto, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with ID: " + projectId));

        if (!project.getTenant().getId().equals(user.getTenant().getId())) {
            throw new AccessDeniedException("You don't have permission to update this project.");
        }

        if (!project.getName().equals(dto.getName()) &&
                projectRepository.existsByNameAndTenant(dto.getName(), user.getTenant())) {
            throw new ProjectAlreadyExistsException("Another project with the same name exists.");
        }

        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setStartDate(dto.getStartDate());
        project.setEndDate(dto.getEndDate());
        project.setStatus(dto.getStatus());

        return projectMapper.toResponseDTO(projectRepository.save(project));
    }

    @Override
    public void deleteProject(Long id, User user) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!project.getTenant().getId().equals(user.getTenant().getId())) {
            throw new AccessDeniedException("You don't have permission to delete this project.");
        }

        project.setDeleted(true);
        projectRepository.save(project);
    }

    @Override
    public ProjectResponseDTO getProjectById(Long projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with ID: " + projectId));

        if (!project.getTenant().getId().equals(user.getTenant().getId())) {
            throw new AccessDeniedException("Access denied: You don't own this project.");
        }

        return projectMapper.toResponseDTO(project);
    }

    @Override
    public Page<ProjectResponseDTO> getProjectsForTenantPaginated(
            User user, int page, int size, String name, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return projectRepository
                .findByTenantIdAndNameContainingIgnoreCaseAndStatusContainingIgnoreCaseAndIsDeletedFalse(
                        user.getTenant().getId(),
                        name != null ? name : "",
                        status != null ? status : "",
                        pageable)
                .map(projectMapper::toResponseDTO);
    }
}