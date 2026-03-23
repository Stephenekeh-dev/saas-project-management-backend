package com.steve.saasapp.repository;

import com.steve.saasapp.model.Project;
import com.steve.saasapp.model.Task;
import com.steve.saasapp.model.Tenant;
import com.steve.saasapp.model.User;
import com.steve.saasapp.model.enums.TaskPriority;
import com.steve.saasapp.model.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TaskRepository")
class TaskRepositoryTest {

    @Autowired private TaskRepository taskRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;

    private Project project;
    private User user;
    private Task task1;
    private Task task2;
    private Task deletedTask;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        Tenant tenant = tenantRepository.save(Tenant.builder().name("Acme Corp").build());

        user = userRepository.save(User.builder()
                .name("Alice").email("alice@acme.com")
                .password("hashed").tenant(tenant).role(User.Role.ADMIN).build());

        project = projectRepository.save(Project.builder()
                .name("Apollo").tenant(tenant).createdBy(user)
                .isDeleted(false).build());

        task1 = taskRepository.save(Task.builder()
                .title("Design UI").description("Mockups")
                .status(TaskStatus.TO_DO).priority(TaskPriority.HIGH)
                .project(project).assignedTo(user).isDeleted(false).build());

        task2 = taskRepository.save(Task.builder()
                .title("Write tests").description("Unit tests")
                .status(TaskStatus.IN_PROGRESS).priority(TaskPriority.MEDIUM)
                .project(project).assignedTo(user).isDeleted(false).build());

        deletedTask = taskRepository.save(Task.builder()
                .title("Old task").description("Deprecated")
                .status(TaskStatus.DONE).priority(TaskPriority.LOW)
                .project(project).assignedTo(user).isDeleted(true).build());
    }

    @Nested
    @DisplayName("findByProject")
    class FindByProject {

        @Test
        @DisplayName("returns all tasks including deleted for the project")
        void returnsAllTasksForProject() {
            List<Task> results = taskRepository.findByProject(project);
            assertThat(results).hasSize(3);
        }
    }

    @Nested
    @DisplayName("findByProjectAndIsDeletedFalse")
    class FindByProjectAndIsDeletedFalse {

        @Test
        @DisplayName("returns only non-deleted tasks for project")
        void returnsNonDeletedTasksOnly() {
            List<Task> results = taskRepository.findByProjectAndIsDeletedFalse(project);

            assertThat(results).hasSize(2);
            assertThat(results).extracting(Task::getTitle)
                    .containsExactlyInAnyOrder("Design UI", "Write tests")
                    .doesNotContain("Old task");
        }
    }

    @Nested
    @DisplayName("findByAssignedTo")
    class FindByAssignedTo {

        @Test
        @DisplayName("returns all tasks assigned to user including deleted")
        void returnsAllAssignedTasks() {
            List<Task> results = taskRepository.findByAssignedTo(user);
            assertThat(results).hasSize(3);
        }
    }

    @Nested
    @DisplayName("findByAssignedToAndIsDeletedFalse")
    class FindByAssignedToAndIsDeletedFalse {

        @Test
        @DisplayName("returns only non-deleted tasks assigned to user")
        void returnsNonDeletedAssignedTasksOnly() {
            List<Task> results = taskRepository.findByAssignedToAndIsDeletedFalse(user);

            assertThat(results).hasSize(2);
            assertThat(results).extracting(Task::getTitle)
                    .doesNotContain("Old task");
        }
    }

    @Nested
    @DisplayName("findByIdAndIsDeletedFalse")
    class FindByIdAndIsDeletedFalse {

        @Test
        @DisplayName("returns task when it exists and is not deleted")
        void returnsTaskWhenNotDeleted() {
            Optional<Task> result = taskRepository.findByIdAndIsDeletedFalse(task1.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("Design UI");
        }

        @Test
        @DisplayName("returns empty when task is soft-deleted")
        void returnsEmptyWhenDeleted() {
            Optional<Task> result = taskRepository.findByIdAndIsDeletedFalse(deletedTask.getId());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty when task does not exist")
        void returnsEmptyForNonExistentId() {
            Optional<Task> result = taskRepository.findByIdAndIsDeletedFalse(999L);

            assertThat(result).isEmpty();
        }
    }
}
