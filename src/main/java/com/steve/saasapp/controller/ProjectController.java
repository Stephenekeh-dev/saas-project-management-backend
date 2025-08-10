package com.steve.saasapp.controller;

import com.steve.saasapp.dto.ProjectRequestDTO;
import com.steve.saasapp.dto.ProjectResponseDTO;
import com.steve.saasapp.model.User;
import com.steve.saasapp.security.CurrentUser;
import com.steve.saasapp.security.CustomUserDetails;
import com.steve.saasapp.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponseDTO> createProject(@Valid @RequestBody ProjectRequestDTO request) {
        // Use SecurityContextHolder once for consistency
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userDetails.getUser();

        ProjectResponseDTO createdProject = projectService.createProject(request, user);
        return ResponseEntity.ok(createdProject);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProjectResponseDTO>> getAllProjects(@AuthenticationPrincipal CustomUserDetails userDetails) {
        // 💡 Use @AuthenticationPrincipal directly — no need for custom @CurrentUser unless necessary
        User user = userDetails.getUser();

        List<ProjectResponseDTO> projects = projectService.getProjectsForTenant(user);
        return ResponseEntity.ok(projects);
    }
    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponseDTO> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectRequestDTO request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        ProjectResponseDTO updatedProject = projectService.updateProject(projectId, request, user);
        return ResponseEntity.ok(updatedProject);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProject(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        projectService.deleteProject(id, user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Project deleted successfully.");
        return ResponseEntity.ok(response);  // Return 200 OK with message
    }
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponseDTO> getProjectById(
            @PathVariable Long projectId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        User user = userDetails.getUser();
        ProjectResponseDTO project = projectService.getProjectById(projectId, user);
        return ResponseEntity.ok(project);
    }
    @GetMapping
    public ResponseEntity<Page<ProjectResponseDTO>> getAllProjects(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status
    ) {
        User user = userDetails.getUser();
        Page<ProjectResponseDTO> paginatedProjects =
                projectService.getProjectsForTenantPaginated(user, page, size, name, status);
        return ResponseEntity.ok(paginatedProjects);
    }



}
