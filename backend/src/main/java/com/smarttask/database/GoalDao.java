package com.smarttask.database;

import com.smarttask.datastructures.DynamicArray;
import com.smarttask.model.Goal;
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
public class GoalDao {

    private final DataSource dataSource;

    public GoalDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DynamicArray<Goal> findByAimId(Long aimId, Long userId) {
        String sql = "SELECT goal_id, aim_id, user_id, title, description, deadline, created_at FROM goals WHERE aim_id = ? AND user_id = ? ORDER BY created_at ASC";
        return queryList(sql, aimId, userId);
    }

    public Optional<Goal> findByIdAndUserId(Long goalId, Long userId) {
        String sql = "SELECT goal_id, aim_id, user_id, title, description, deadline, created_at FROM goals WHERE goal_id = ? AND user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, goalId);
            statement.setLong(2, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to find goal.", ex);
        }
    }

    public Goal save(Goal goal) {
        if (goal.getId() == null) {
            return insert(goal);
        }
        return update(goal);
    }

    public void delete(Long goalId, Long userId) {
        String sql = "DELETE FROM goals WHERE goal_id = ? AND user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, goalId);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete goal.", ex);
        }
    }

    private DynamicArray<Goal> queryList(String sql, Long first, Long second) {
        DynamicArray<Goal> goals = new DynamicArray<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, first);
            statement.setLong(2, second);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    goals.add(map(rs));
                }
            }
            return goals;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load goals.", ex);
        }
    }

    private Goal insert(Goal goal) {
        String sql = "INSERT INTO goals (aim_id, user_id, title, description, deadline, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        LocalDateTime createdAt = goal.getCreatedAt() == null ? LocalDateTime.now() : goal.getCreatedAt();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, goal.getAimId());
            statement.setLong(2, goal.getUserId());
            statement.setString(3, goal.getTitle());
            statement.setString(4, goal.getDescription());
            if (goal.getDeadline() == null) {
                statement.setNull(5, Types.DATE);
            } else {
                statement.setDate(5, Date.valueOf(goal.getDeadline()));
            }
            statement.setTimestamp(6, Timestamp.valueOf(createdAt));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    goal.setId(keys.getLong(1));
                }
            }
            goal.setCreatedAt(createdAt);
            return goal;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to insert goal.", ex);
        }
    }

    private Goal update(Goal goal) {
        String sql = "UPDATE goals SET title = ?, description = ?, deadline = ? WHERE goal_id = ? AND user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, goal.getTitle());
            statement.setString(2, goal.getDescription());
            if (goal.getDeadline() == null) {
                statement.setNull(3, Types.DATE);
            } else {
                statement.setDate(3, Date.valueOf(goal.getDeadline()));
            }
            statement.setLong(4, goal.getId());
            statement.setLong(5, goal.getUserId());
            statement.executeUpdate();
            return goal;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to update goal.", ex);
        }
    }

    private Goal map(ResultSet rs) throws SQLException {
        Goal goal = new Goal();
        goal.setId(rs.getLong("goal_id"));
        goal.setAimId(rs.getLong("aim_id"));
        goal.setUserId(rs.getLong("user_id"));
        goal.setTitle(rs.getString("title"));
        goal.setDescription(rs.getString("description"));
        Date deadline = rs.getDate("deadline");
        goal.setDeadline(deadline == null ? null : deadline.toLocalDate());
        Timestamp created = rs.getTimestamp("created_at");
        goal.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return goal;
    }
}
