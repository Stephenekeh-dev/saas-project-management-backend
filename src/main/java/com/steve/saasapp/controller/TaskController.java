package com.steve.saasapp.controller;

import com.steve.saasapp.dto.TaskRequestDTO;
import com.steve.saasapp.dto.TaskResponseDTO;
import com.steve.saasapp.model.User;
import com.steve.saasapp.security.CustomUserDetails;
import com.steve.saasapp.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/projects/{projectId}/tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TaskResponseDTO> createTask(
            @PathVariable Long projectId,
            @RequestBody TaskRequestDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(projectId, dto, userDetails.getUser()));
    }

    @GetMapping("/projects/{projectId}/tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'MEMBER')")
    public ResponseEntity<List<TaskResponseDTO>> getTasksByProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(taskService.getTasksByProject(projectId, userDetails.getUser()));
    }

    @PutMapping("/tasks/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'MEMBER')")
    public ResponseEntity<TaskResponseDTO> updateTask(
            @PathVariable Long taskId,
            @RequestBody TaskRequestDTO taskRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(taskService.updateTask(taskId, taskRequest, userDetails.getUser()));
    }

    @DeleteMapping("/tasks/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        taskService.deleteTask(id, userDetails.getUser());
        return ResponseEntity.ok(Collections.singletonMap("message", "Task deleted successfully"));
    }
}