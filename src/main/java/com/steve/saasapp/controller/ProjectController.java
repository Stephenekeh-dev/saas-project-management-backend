package com.steve.saasapp.controller;

import com.steve.saasapp.dto.ProjectRequestDTO;
import com.steve.saasapp.dto.ProjectResponseDTO;
import com.steve.saasapp.model.User;
import com.steve.saasapp.security.CustomUserDetails;
import com.steve.saasapp.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ProjectResponseDTO> createProject(
            @Valid @RequestBody ProjectRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(projectService.createProject(request, userDetails.getUser()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'MEMBER')")
    public ResponseEntity<List<ProjectResponseDTO>> getAllProjects(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(projectService.getProjectsForTenant(userDetails.getUser()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'MEMBER')")
    public ResponseEntity<Page<ProjectResponseDTO>> getProjectsPaginated(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(
                projectService.getProjectsForTenantPaginated(
                        userDetails.getUser(), page, size, name, status));
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'MEMBER')")
    public ResponseEntity<ProjectResponseDTO> getProjectById(
            @PathVariable Long projectId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(projectService.getProjectById(projectId, userDetails.getUser()));
    }

    @PutMapping("/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ProjectResponseDTO> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                projectService.updateProject(projectId, request, userDetails.getUser()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteProject(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        projectService.deleteProject(id, userDetails.getUser());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Project deleted successfully.");
        return ResponseEntity.ok(response);
    }
}