package com.steve.saasapp.dto;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TaskResponseDTO {
    private Long id;
    private String title;
    private String description;
    private String status;
    private String priority;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private Long projectId;
    private Long assignedToUserId;
}