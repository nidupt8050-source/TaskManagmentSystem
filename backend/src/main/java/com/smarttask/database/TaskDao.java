package com.smarttask.database;

import com.smarttask.datastructures.DynamicArray;
import com.smarttask.model.Priority;
import com.smarttask.model.Status;
import com.smarttask.model.Task;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class TaskDao {

    private final DataSource dataSource;

    public TaskDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DynamicArray<Task> findByUserIdOrderByDeadlineAsc(Long userId) {
        String sql = """
            SELECT task_id, user_id, objective_id, title, description, priority, status,
                   deadline, created_at, completed_at
            FROM tasks
            WHERE user_id = ?
            ORDER BY deadline ASC
            """;
        DynamicArray<Task> tasks = new DynamicArray<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapTask(rs));
                }
            }
            return tasks;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load tasks.", ex);
        }
    }

    public DynamicArray<Task> findByObjectiveId(Long objectiveId, Long userId) {
        String sql = """
            SELECT task_id, user_id, objective_id, title, description, priority, status,
                   deadline, created_at, completed_at
            FROM tasks
            WHERE objective_id = ? AND user_id = ?
            ORDER BY deadline ASC
            """;
        DynamicArray<Task> tasks = new DynamicArray<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, objectiveId);
            statement.setLong(2, userId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapTask(rs));
                }
            }
            return tasks;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load objective tasks.", ex);
        }
    }

    public Optional<Task> findByIdAndUserId(Long taskId, Long userId) {
        String sql = """
            SELECT task_id, user_id, objective_id, title, description, priority, status,
                   deadline, created_at, completed_at
            FROM tasks
            WHERE task_id = ? AND user_id = ?
            """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, taskId);
            statement.setLong(2, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapTask(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to find task.", ex);
        }
    }

    public Task save(Task task) {
        if (task.getId() == null) {
            return insert(task);
        }
        return update(task);
    }

    public void delete(Task task) {
        String sql = "DELETE FROM tasks WHERE task_id = ? AND user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, task.getId());
            statement.setLong(2, task.getUserId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete task.", ex);
        }
    }

    private Task insert(Task task) {
        String sql = """
            INSERT INTO tasks
            (user_id, objective_id, title, description, priority, status, deadline, created_at, completed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        LocalDateTime createdAt = task.getCreatedAt() == null ? LocalDateTime.now() : task.getCreatedAt();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindTask(statement, task, createdAt);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    task.setId(keys.getLong(1));
                }
            }
            task.setCreatedAt(createdAt);
            return task;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to insert task.", ex);
        }
    }

    private Task update(Task task) {
        String sql = """
            UPDATE tasks
            SET objective_id = ?, title = ?, description = ?, priority = ?, status = ?,
                deadline = ?, completed_at = ?
            WHERE task_id = ? AND user_id = ?
            """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (task.getObjectiveId() == null) {
                statement.setNull(1, Types.BIGINT);
            } else {
                statement.setLong(1, task.getObjectiveId());
            }
            statement.setString(2, task.getTitle());
            statement.setString(3, task.getDescription());
            statement.setString(4, task.getPriority().name());
            statement.setString(5, task.getStatus().name());
            statement.setDate(6, Date.valueOf(task.getDeadline()));
            if (task.getCompletedAt() == null) {
                statement.setNull(7, Types.TIMESTAMP);
            } else {
                statement.setTimestamp(7, Timestamp.valueOf(task.getCompletedAt()));
            }
            statement.setLong(8, task.getId());
            statement.setLong(9, task.getUserId());
            statement.executeUpdate();
            return task;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to update task.", ex);
        }
    }

    private void bindTask(PreparedStatement statement, Task task, LocalDateTime createdAt) throws SQLException {
        statement.setLong(1, task.getUserId());
        if (task.getObjectiveId() == null) {
            statement.setNull(2, Types.BIGINT);
        } else {
            statement.setLong(2, task.getObjectiveId());
        }
        statement.setString(3, task.getTitle());
        statement.setString(4, task.getDescription());
        statement.setString(5, task.getPriority().name());
        statement.setString(6, task.getStatus().name());
        statement.setDate(7, Date.valueOf(task.getDeadline()));
        statement.setTimestamp(8, Timestamp.valueOf(createdAt));
        if (task.getCompletedAt() == null) {
            statement.setNull(9, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(9, Timestamp.valueOf(task.getCompletedAt()));
        }
    }

    private Task mapTask(ResultSet rs) throws SQLException {
        Task task = new Task();
        task.setId(rs.getLong("task_id"));
        task.setUserId(rs.getLong("user_id"));
        long objectiveId = rs.getLong("objective_id");
        task.setObjectiveId(rs.wasNull() ? null : objectiveId);
        task.setTitle(rs.getString("title"));
        task.setDescription(rs.getString("description"));
        task.setPriority(Priority.valueOf(rs.getString("priority")));
        task.setStatus(Status.valueOf(rs.getString("status")));
        Date deadline = rs.getDate("deadline");
        task.setDeadline(deadline == null ? null : deadline.toLocalDate());
        Timestamp created = rs.getTimestamp("created_at");
        task.setCreatedAt(created == null ? null : created.toLocalDateTime());
        Timestamp completed = rs.getTimestamp("completed_at");
        task.setCompletedAt(completed == null ? null : completed.toLocalDateTime());
        return task;
    }
}
