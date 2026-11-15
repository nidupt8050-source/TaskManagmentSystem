package com.smarttask.database;

import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Ensures required tables exist using JDBC (no Hibernate DDL).
 */
@Configuration
public class DatabaseConfig {

    private final DataSource dataSource;

    public DatabaseConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void initializeSchema() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("""
                CREATE TABLE IF NOT EXISTS users (
                  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  name VARCHAR(100) NOT NULL,
                  email VARCHAR(150) NOT NULL UNIQUE,
                  password VARCHAR(255) NOT NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS aims (
                  aim_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  user_id BIGINT NOT NULL,
                  title VARCHAR(200) NOT NULL,
                  description VARCHAR(1000) NULL,
                  deadline DATE NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  CONSTRAINT fk_aims_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                )
                """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS goals (
                  goal_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  aim_id BIGINT NOT NULL,
                  user_id BIGINT NOT NULL,
                  title VARCHAR(200) NOT NULL,
                  description VARCHAR(1000) NULL,
                  deadline DATE NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  CONSTRAINT fk_goals_aim FOREIGN KEY (aim_id) REFERENCES aims(aim_id) ON DELETE CASCADE,
                  CONSTRAINT fk_goals_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                )
                """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS objectives (
                  objective_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  goal_id BIGINT NOT NULL,
                  user_id BIGINT NOT NULL,
                  title VARCHAR(200) NOT NULL,
                  description VARCHAR(1000) NULL,
                  deadline DATE NULL,
                  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  CONSTRAINT fk_objectives_goal FOREIGN KEY (goal_id) REFERENCES goals(goal_id) ON DELETE CASCADE,
                  CONSTRAINT fk_objectives_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                )
                """);

            try {
                statement.execute("ALTER TABLE objectives ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");
            } catch (SQLException ignored) {
                // Column already exists.
            }

            statement.execute("""
                CREATE TABLE IF NOT EXISTS tasks (
                  task_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  user_id BIGINT NOT NULL,
                  objective_id BIGINT NULL,
                  title VARCHAR(150) NOT NULL,
                  description VARCHAR(1000) NULL,
                  priority VARCHAR(20) NOT NULL,
                  status VARCHAR(20) NOT NULL,
                  deadline DATE NOT NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  completed_at DATETIME NULL,
                  CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                )
                """);

            // Upgrade older databases that already have tasks without objective_id.
            try {
                statement.execute("ALTER TABLE tasks ADD COLUMN objective_id BIGINT NULL");
            } catch (SQLException ignored) {
                // Column already exists.
            }

            statement.execute("""
                CREATE TABLE IF NOT EXISTS password_reset_tokens (
                  token_hash VARCHAR(64) PRIMARY KEY,
                  user_id BIGINT NOT NULL UNIQUE,
                  expires_at DATETIME(6) NOT NULL,
                  CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                )
                """);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to initialize SmartTask database schema.", ex);
        }
    }
}
