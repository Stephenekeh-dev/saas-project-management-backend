package com.steve.saasapp.controller;

import com.steve.saasapp.dto.TaskRequestDTO;
import com.steve.saasapp.dto.TaskResponseDTO;
import com.steve.saasapp.model.User;
import com.steve.saasapp.security.CustomUserDetails;
import com.steve.saasapp.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<TaskResponseDTO> createTask(
            @PathVariable Long projectId,
            @RequestBody TaskRequestDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        User user = userDetails.getUser(); // Now `user.getTenant()` won't throw NPE
        TaskResponseDTO created = taskService.createTask(projectId, dto, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }



    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<List<TaskResponseDTO>> getTasksByProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        User user = userDetails.getUser(); // Extract the User entity from your custom user details

        List<TaskResponseDTO> tasks = taskService.getTasksByProject(projectId, user);
        return ResponseEntity.ok(tasks);
    }



    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<TaskResponseDTO> updateTask(
            @PathVariable Long taskId,
            @RequestBody TaskRequestDTO taskRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        User user = userDetails.getUser(); // 🔐 Now you have the authenticated user
        TaskResponseDTO updatedTask = taskService.updateTask(taskId, taskRequest, user);
        return ResponseEntity.ok(updatedTask);
    }


    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        taskService.deleteTask(id, user);
        return ResponseEntity.ok(Collections.singletonMap("message", "Task deleted successfully"));
    }
}