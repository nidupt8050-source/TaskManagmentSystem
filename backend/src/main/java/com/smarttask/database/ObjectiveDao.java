package com.smarttask.database;

import com.smarttask.datastructures.DynamicArray;
import com.smarttask.model.Objective;
import com.smarttask.model.ObjectiveStatus;
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
public class ObjectiveDao {

    private final DataSource dataSource;

    public ObjectiveDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DynamicArray<Objective> findByGoalId(Long goalId, Long userId) {
        String sql = """
            SELECT objective_id, goal_id, user_id, title, description, deadline, status, created_at
            FROM objectives WHERE goal_id = ? AND user_id = ? ORDER BY created_at ASC
            """;
        return queryList(sql, goalId, userId);
    }

    public DynamicArray<Objective> findByUserId(Long userId) {
        String sql = """
            SELECT objective_id, goal_id, user_id, title, description, deadline, status, created_at
            FROM objectives WHERE user_id = ? ORDER BY title ASC
            """;
        DynamicArray<Objective> objectives = new DynamicArray<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    objectives.add(map(rs));
                }
            }
            return objectives;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load user objectives.", ex);
        }
    }

    public Optional<Objective> findByIdAndUserId(Long objectiveId, Long userId) {
        String sql = """
            SELECT objective_id, goal_id, user_id, title, description, deadline, status, created_at
            FROM objectives WHERE objective_id = ? AND user_id = ?
            """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, objectiveId);
            statement.setLong(2, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to find objective.", ex);
        }
    }

    public Objective save(Objective objective) {
        if (objective.getId() == null) {
            return insert(objective);
        }
        return update(objective);
    }

    public void delete(Long objectiveId, Long userId) {
        String sql = "DELETE FROM objectives WHERE objective_id = ? AND user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, objectiveId);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete objective.", ex);
        }
    }

    private DynamicArray<Objective> queryList(String sql, Long first, Long second) {
        DynamicArray<Objective> objectives = new DynamicArray<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, first);
            statement.setLong(2, second);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    objectives.add(map(rs));
                }
            }
            return objectives;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load objectives.", ex);
        }
    }

    private Objective insert(Objective objective) {
        String sql = """
            INSERT INTO objectives (goal_id, user_id, title, description, deadline, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        LocalDateTime createdAt = objective.getCreatedAt() == null ? LocalDateTime.now() : objective.getCreatedAt();
        ObjectiveStatus status = objective.getStatus() == null ? ObjectiveStatus.ACTIVE : objective.getStatus();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, objective.getGoalId());
            statement.setLong(2, objective.getUserId());
            statement.setString(3, objective.getTitle());
            statement.setString(4, objective.getDescription());
            if (objective.getDeadline() == null) {
                statement.setNull(5, Types.DATE);
            } else {
                statement.setDate(5, Date.valueOf(objective.getDeadline()));
            }
            statement.setString(6, status.name());
            statement.setTimestamp(7, Timestamp.valueOf(createdAt));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    objective.setId(keys.getLong(1));
                }
            }
            objective.setStatus(status);
            objective.setCreatedAt(createdAt);
            return objective;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to insert objective.", ex);
        }
    }

    private Objective update(Objective objective) {
        String sql = """
            UPDATE objectives
            SET title = ?, description = ?, deadline = ?, status = ?
            WHERE objective_id = ? AND user_id = ?
            """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, objective.getTitle());
            statement.setString(2, objective.getDescription());
            if (objective.getDeadline() == null) {
                statement.setNull(3, Types.DATE);
            } else {
                statement.setDate(3, Date.valueOf(objective.getDeadline()));
            }
            statement.setString(4, objective.getStatus() == null ? ObjectiveStatus.ACTIVE.name() : objective.getStatus().name());
            statement.setLong(5, objective.getId());
            statement.setLong(6, objective.getUserId());
            statement.executeUpdate();
            return objective;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to update objective.", ex);
        }
    }

    private Objective map(ResultSet rs) throws SQLException {
        Objective objective = new Objective();
        objective.setId(rs.getLong("objective_id"));
        objective.setGoalId(rs.getLong("goal_id"));
        objective.setUserId(rs.getLong("user_id"));
        objective.setTitle(rs.getString("title"));
        objective.setDescription(rs.getString("description"));
        Date deadline = rs.getDate("deadline");
        objective.setDeadline(deadline == null ? null : deadline.toLocalDate());
        String status = rs.getString("status");
        objective.setStatus(status == null || status.isBlank() ? ObjectiveStatus.ACTIVE : ObjectiveStatus.valueOf(status));
        Timestamp created = rs.getTimestamp("created_at");
        objective.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return objective;
    }
}
