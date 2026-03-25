package com.steve.saasapp.repository;

import com.steve.saasapp.model.Project;
import com.steve.saasapp.model.Tenant;
import com.steve.saasapp.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ProjectRepository")
class ProjectRepositoryTest {

    @Autowired private ProjectRepository projectRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;

    private Tenant tenantA;
    private Tenant tenantB;
    private User userA;
    private Project projectA1;
    private Project projectA2;
    private Project projectB1;

    @BeforeEach
    void setUp() {
        projectRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        tenantA = tenantRepository.save(Tenant.builder().name("Acme Corp").build());
        tenantB = tenantRepository.save(Tenant.builder().name("Globex Inc").build());

        userA = userRepository.save(User.builder()
                .name("Alice").email("alice@acme.com")
                .password("hashed").tenant(tenantA).role(User.Role.ADMIN).build());

        projectA1 = projectRepository.save(Project.builder()
                .name("Apollo").description("First project")
                .status("TODO").tenant(tenantA).createdBy(userA)
                .isDeleted(false).build());

        projectA2 = projectRepository.save(Project.builder()
                .name("Gemini").description("Second project")
                .status("IN_PROGRESS").tenant(tenantA).createdBy(userA)
                .isDeleted(false).build());

        projectB1 = projectRepository.save(Project.builder()
                .name("Phoenix").description("Globex project")
                .status("TODO").tenant(tenantB).createdBy(userA)
                .isDeleted(false).build());
    }

    @Nested
    @DisplayName("findByTenantAndIsDeletedFalse")
    class FindByTenantAndIsDeletedFalse {

        @Test
        @DisplayName("returns only non-deleted projects for the given tenant")
        void returnsNonDeletedForTenant() {
            // Soft-delete one project
            projectA1.setDeleted(true);
            projectRepository.save(projectA1);

            List<Project> results = projectRepository.findByTenantAndIsDeletedFalse(tenantA);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo("Gemini");
        }

        @Test
        @DisplayName("does not return projects from other tenants")
        void excludesOtherTenants() {
            List<Project> results = projectRepository.findByTenantAndIsDeletedFalse(tenantA);

            assertThat(results).extracting(Project::getName)
                    .containsExactlyInAnyOrder("Apollo", "Gemini")
                    .doesNotContain("Phoenix");
        }
    }

    @Nested
    @DisplayName("existsByNameAndTenant")
    class ExistsByNameAndTenant {

        @Test
        @DisplayName("returns true when project with same name exists for tenant")
        void returnsTrueForExistingName() {
            assertThat(projectRepository.existsByNameAndTenant("Apollo", tenantA)).isTrue();
        }

        @Test
        @DisplayName("returns false when name does not exist for tenant")
        void returnsFalseForNewName() {
            assertThat(projectRepository.existsByNameAndTenant("Titan", tenantA)).isFalse();
        }

        @Test
        @DisplayName("returns false when name exists but for a different tenant")
        void returnsFalseForDifferentTenant() {
            // Phoenix belongs to tenantB, not tenantA
            assertThat(projectRepository.existsByNameAndTenant("Phoenix", tenantA)).isFalse();
        }
    }

    @Nested
    @DisplayName("findByIdAndIsDeletedFalse")
    class FindByIdAndIsDeletedFalse {

        @Test
        @DisplayName("returns project when it exists and is not deleted")
        void returnsProjectWhenNotDeleted() {
            Optional<Project> result = projectRepository.findByIdAndIsDeletedFalse(projectA1.getId());
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Apollo");
        }

        @Test
        @DisplayName("returns empty when project is soft-deleted")
        void returnsEmptyWhenDeleted() {
            projectA1.setDeleted(true);
            projectRepository.save(projectA1);

            Optional<Project> result = projectRepository.findByIdAndIsDeletedFalse(projectA1.getId());
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByTenantIdAndNameContainingIgnoreCaseAndStatusContainingIgnoreCaseAndIsDeletedFalse")
    class FindPaginated {

        @Test
        @DisplayName("returns all non-deleted tenant projects when filters are empty")
        void emptyFilters_returnsAll() {
            PageRequest pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
            Page<Project> result = projectRepository
                    .findByTenantIdAndNameContainingIgnoreCaseAndStatusContainingIgnoreCaseAndIsDeletedFalse(
                            tenantA.getId(), "", "", pageable);

            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("filters by name case-insensitively")
        void filterByName_caseInsensitive() {
            PageRequest pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
            Page<Project> result = projectRepository
                    .findByTenantIdAndNameContainingIgnoreCaseAndStatusContainingIgnoreCaseAndIsDeletedFalse(
                            tenantA.getId(), "apollo", "", pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Apollo");
        }

        @Test
        @DisplayName("filters by status")
        void filterByStatus() {
            PageRequest pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
            Page<Project> result = projectRepository
                    .findByTenantIdAndNameContainingIgnoreCaseAndStatusContainingIgnoreCaseAndIsDeletedFalse(
                            tenantA.getId(), "", "IN_PROGRESS", pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Gemini");
        }

        @Test
        @DisplayName("excludes soft-deleted projects from paginated results")
        void excludesDeleted() {
            projectA1.setDeleted(true);
            projectRepository.save(projectA1);

            PageRequest pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
            Page<Project> result = projectRepository
                    .findByTenantIdAndNameContainingIgnoreCaseAndStatusContainingIgnoreCaseAndIsDeletedFalse(
                            tenantA.getId(), "", "", pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("does not return projects from other tenants")
        void excludesOtherTenants() {
            PageRequest pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
            Page<Project> result = projectRepository
                    .findByTenantIdAndNameContainingIgnoreCaseAndStatusContainingIgnoreCaseAndIsDeletedFalse(
                            tenantA.getId(), "", "", pageable);

            assertThat(result.getContent())
                    .extracting(Project::getName)
                    .doesNotContain("Phoenix");
        }
    }
}