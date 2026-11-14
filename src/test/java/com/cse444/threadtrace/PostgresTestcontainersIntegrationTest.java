package com.cse444.threadtrace;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresTestcontainersIntegrationTest {

    static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @Test
    @EnabledIf("isDockerAvailable")
    @DisplayName("Testcontainers PostgreSQL 16 container starts and runs successfully when Docker is available")
    void postgresContainerRuns() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("threadtrace")
                .withUsername("threadtrace")
                .withPassword("threadtrace")) {
            postgres.start();
            assertThat(postgres.isRunning()).isTrue();
            assertThat(postgres.getJdbcUrl()).contains("postgresql");
        }
    }
}
