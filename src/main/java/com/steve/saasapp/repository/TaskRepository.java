package com.steve.saasapp.repository;

import com.steve.saasapp.model.Task;
import com.steve.saasapp.model.Project;
import com.steve.saasapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProject(Project project);
    List<Task> findByAssignedTo(User user);
    List<Task> findByProjectAndIsDeletedFalse(Project project);
    List<Task> findByAssignedToAndIsDeletedFalse(User user);
    Optional<Task> findByIdAndIsDeletedFalse(Long id);

}
