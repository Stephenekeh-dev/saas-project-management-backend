package com.steve.saasapp.service;

import com.steve.saasapp.dto.ProjectRequestDTO;
import com.steve.saasapp.dto.ProjectResponseDTO;
import com.steve.saasapp.model.User;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProjectService {

    ProjectResponseDTO createProject(ProjectRequestDTO dto, User creator);

    List<ProjectResponseDTO> getProjectsForTenant(User user);
    ProjectResponseDTO updateProject(Long projectId, ProjectRequestDTO dto, User user);
    void deleteProject(Long projectId, User user);
    ProjectResponseDTO getProjectById(Long projectId, User user);
    Page<ProjectResponseDTO> getProjectsForTenantPaginated(
            User user,
            int page,
            int size,
            String name,
            String status
    );

}
