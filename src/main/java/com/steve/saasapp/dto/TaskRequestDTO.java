package com.steve.saasapp.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskRequestDTO {
    private String title;
    private String description;
    private String status;     // TO_DO, IN_PROGRESS, DONE
    private String priority;   // LOW, MEDIUM, HIGH
    private LocalDate dueDate;
    private Long assignedToUserId;

}