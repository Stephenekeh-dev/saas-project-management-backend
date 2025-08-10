package com.steve.saasapp.model;

import com.steve.saasapp.model.enums.TaskPriority;
import com.steve.saasapp.model.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;



    @Entity
    @Table(name = "tasks")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class Task {



        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(optional = false)
        @JoinColumn(name = "project_id")
        private Project project;

        @ManyToOne
        @JoinColumn(name = "assigned_to")
        private User assignedTo;

        @Column(nullable = false)
        private String title;

        private String description;

        @Enumerated(EnumType.STRING)
        private TaskStatus status;

        @Enumerated(EnumType.STRING)
        private TaskPriority priority;


        private LocalDate dueDate;

        private LocalDateTime createdAt;

        @Builder.Default
        private boolean isDeleted = false;

        @PrePersist
        public void prePersist() {
            this.createdAt = LocalDateTime.now();
        }
    }


