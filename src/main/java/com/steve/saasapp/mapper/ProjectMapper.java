package com.steve.saasapp.mapper;

import com.steve.saasapp.dto.ProjectResponseDTO;
import com.steve.saasapp.model.Project;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    public ProjectResponseDTO toResponseDTO(Project project) {
        ProjectResponseDTO dto = new ProjectResponseDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setDescription(project.getDescription());
        dto.setStartDate(project.getStartDate());
        dto.setEndDate(project.getEndDate());
        dto.setStatus(project.getStatus());

        // ✅ Add the missing fields
        if (project.getTenant() != null) {
            dto.setTenantName(project.getTenant().getName());
        }

        if (project.getCreatedBy() != null) {
            dto.setCreatedBy(project.getCreatedBy().getName());
        }

        dto.setCreatedAt(project.getCreatedAt());

        return dto;
    }

}
