package com.steve.saasapp.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectRequestDTO {
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}